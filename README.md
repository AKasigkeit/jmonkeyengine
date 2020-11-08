This fork attempts to bring more modern OpenGL (OpenGL4+) features to jMonkeyEngine.

## Most important changes:
- Brings ComputeShaders to jme3
- Makes use of VAOs to reduce glVertexAttribPointer and glBindBuffer calls drastically
- Keeps track of currently bound textures / buffers in a different way in an attempt to reduce related state switches
    Including using implementation dependant maximum for VertexBufferBindings, TextureBindings etc to further reduce switches as much as the driver allows
- OpenGL query objects have been implemented (or, well, reworked as the DetailedProfilerState made use of them already)
- Supports MultiDrawIndirect for DrawElementsIndirectCommands and DrawArraysIndirectCommands
- Buffers have been reworked. now supported buffer types:
  VertexBuffer (VBO), UniformBuffer (UBO), ShaderStorageBuffer (SSBO), AtomicCounterBuffer, DispatchIndirectBuffer, DrawIndirectBuffer, QueryBuffer and ParameterBuffer

This fork is under active development and implementations as well as method signatures etc can change at any time.
Also, while i use it heavily myself i probably didnt yet come to try all the different usages, thus i expect bugs to exist

### ComputeShaders:
To make use of them, you first have to create a ComputeShaderFactory:
```java
ComputeShaderFactory factory = ComputeShaderFactory.create(renderer);
```
then you can instanciate ComputeShaders using a .comp file or a String as source:
```java
ComputeShader someShader    = factory.createComputeShader(assetManager, "Shaders/SomeShader.comp", "GLSL430");
ComputeShader anotherShader = factory.createComputeShader(SOURCE_STRING, "GLSL430"); 
```
Uniforms, Buffers and Textures as well as Images can be bound to the ComputeShader:
```java
someShader.setVector3("Direction", Vector3f.UNIT_Y);
someShader.setFloat("Intensity", 2f);
someShader.setTexture("Tex", assetManager.loadTexture("path/to/tex.png"));
```
Because there is no j3md file or similar for a ComputeShader, uniforms cannot be bound to defines and thus defines have to be set manually:
```java
someShader.setDefine("SIZE", VarType.Float, 1f);
someShader.setDefine("SAMPELS", VarType.Int, 32);
```
this however allows for Strings as defines too, example:
```java
someShader.setDefine("READ_CHANNEL", null, "x");
//to use in glsl shader code like:
float val = imageLoad(m_Image, x).READ_CHANNEL;
```
When using the setTexture() method, the provided texture will be bound as texture and can be accessed in the shader using samplers (same as in fragment shaders), this can make use of lods (although textureLod() needs to be used as only FragmentShaders can do automatic TextureLod calculation), MinFilter and MagFilter and no Format has to be specified explicity as the sampler will 
handle all of that. The downside is, that you can only read from and not write to the texture this way.
However when using setImage(), the provided texture will be bound as image and can be accessed via imageLoad() and imageStore(), but only 1 mipmap level can be bound, the format has to be explicity stated (and much less formats are supported) but it enables writes to the data. A setImage call can look like that:
```java
int mipmapLevel = 0;       //bind max size mipmap level
int layers = -1;           //-1 means bind all layers (in case of TextureArray, TextureCubeMap or Texture3D)
boolean useDefines = true; //sets 3 defines in the shader: *NAME*_WIDTH, *NAME*_HEIGHT and *NAME*_FORMAT
someShader.setImage("Input", VarType.Texture2D, tex, Access.ReadOnly, mipmapLevel, layers, useDefines);
```

## Buffers: 
all buffers have static factory methods like:
```java
UniformBuffer ubo            = UniformBuffer.createNewEmpty();
AtomicCounterBuffer acbo     = AtomicCounterBuffer.createWithInitialValues(0, 0);
QueryBuffer qbo              = QueryBuffer.createWithSize(16, true);
ShaderStorageBuffer ssbo     = ShaderStorageBuffer.createNewEmpty();
DrawIndirectBuffer dribo     = DrawIndirectBuffer.createWithCommands(commands);
DispatchIndirectBuffer diibo = DispatchIndirectBuffer.createWithCommand(command);
```
From an OpenGL point of view, different types of buffers dont exist. There is only buffers (which are just a block of memory) and then there is buffer targets which basically are views on that memory. Thus the intention behind the buffer rework was to allow for the same possibilities that OpenGL allows, ie have different views on the same memory. What it basically means is you can allocate some memory, create a VertexBuffer-view and a ShaderStorageBuffer-view on that same memory, use the VertexBuffer as source for vertex positions while binding the ShaderStorageBuffer to a ComputeShader to update the values before they are fetched in the vertex processing stage while rendering.
Since during buffer rework, not only the existing types have been reworked, but new typed have been added (like DrawIndirectBuffer or DispatchIndirectBuffer), DrawCommands can be created on the GPU as well (using a ShaderStorageBuffer-view and DrawIndirectBuffer-view on the same memory) or QueryObject results can be made available to Shaders without loading them back to the CPU (create a QueryBuffer-view and ShaderStorageBuffer-view on the same memory, store the query result in the query buffer and read it in the ComputeShader or FragmentShader via the ShaderStorageBuffer)

To create several views on the same memory, first the buffer to represent that memory, called UntypedBuffer, has to be created. During creation 3 decisions have to be made:
 - #### DirectMode or LazyMode
     LazyMode is the engines default behaviour, ie data is first send to the GPU once it is actually needed. This is usually fine but has some drawbacks like you cannot decide when the data is actually sent (which might cause stalls if too much data gets send at once when it is actually needed) or you cannot upload data in chunks. DirectMode means that all methods called on the UntypedBuffer will have their GL calls to reflect those changes called already once the method returns (of course that doesnt mean those calls already changed the state on the GPU).
 - #### GpuOnly or CpuGpu
     Specifies the memory usage of this UntypedBuffer. GpuOnly allocates memory on the GPU only, while CpuGpu will keep the data stored on the CPU as well
 - #### BufferData or StorageAPI
     BufferData is the default usage. It is supported by all platforms and specifies how the data will be used (static, dyanamic or streamed). StorageAPI is not supported on old hardware but is needed when you want to map the buffer 

An example of how to create an GPU-Only UntypedBuffer in DirectMode with BufferData.StaticDraw and 2 views on it:
```java
UntypedBuffer buffer = UntypedBuffer.createNewBufferDataDirect(MemoryMode.GpuOnly, renderer, BufferDataUsage.StaticDraw);
buffer.initialize(someSize);
ShaderStorageBuffer ssbo = buffer.asShaderStorageBuffer(null);
VertexBuffer posBuffer = buffer.asVertexBuffer(Type.Position, Format.Float, 3, 12, 0); //3 floats, the stride between the attributes is 12 bytes and their offset is 0 bytes
```
then the ShaderStorageBuffer ssbo can be bound to a ComputeShader to write data to it, while the VertexBuffer can be set on a Mesh to use the data as inPosition in the VertexShader (have to take into account the layout for ssbo).

### PersistentlyMappedBuffers

Mapping a buffer basically means to get a pointer to the GPU memory and directly write to it instead of writing data to the CPU somewhere just to copy it all to the GPU later. This has the downside that you need to get that pointer from OpenGL and in most modern implementations this requires a client-server sync (all prior gl calls have to make it to the driver thread so it can give you the requested pointer) and since any kind of sync is slow, persistently mapped buffers have been introduced which are mapped once and then kept.
A RingBuffer interface has been added with the SingleBufferRingBuffer and MultiBufferRingBuffer implementations that use Persistently Mapped Buffers to stream data to the GPU while providing synchronization and fencing out of the box to make sure you dont write data to a block of the buffer that the GPU currently reads from. 

An example of how to use PersistentlyMappedBuffers:
```java
//do once during initialization
int numBlocks = 3;
int bytesPerBlock = 1024;
RingBuffer buffer = new SingleBufferRingBuffer(RENDER_MANAGER.getRenderer(), bytesPerBlock, numBlocks);
            
//each frame grab the next block (potentially has to wait in case the block is still used by the GPU)
RingBufferBlock block = buffer.next();
//and write the data (up to 'bytesPerBlock' bytes)
block.putInt(123);

//now use that data for rendering or for a compute shader
...

//after the calls have been made that use the data on the GPU, mark the block finished
//this means when calling buffer.next() and it would return this block, it is made sure all GPU commands have finished that were called prior to calling finish() on this block
block.finish();
```

## SyncObjects

SyncObjects are basically markers that can be placed in the GPU queue to later query their state, which is either signaled or not. This means you can do some GL calls, place a SyncObject and when checking its state you know that all work on the GPU has finished that was queued prior to placing the SyncObject in case the state returns signaled. In case it returns unsignaled you know the GPU is still busy processing the GL commands that were queued. 

An example of how to use SyncObjects:
```java
//do some GPU calls
...
            
//then place sync
syncObj = new SyncObject();
renderer.placeSyncObject(syncObj);
            
//the next frame or some other time later check the state
Signal signal = renderer.checkSyncObject(syncObj);
// if signal is Signal.AlreadySignaled or Signal.ConditionSatisfied, all prior work has finished
// else (signal is Signal.WaitFailed or Signal.TimeoutExpired), the GPU is still busy doing the work
```

## QueryObjects

QueryObjects can be used to query the GPU for some specified values, for example: the number of samples that passed depth and stencil tests when rendering a mesh, or the amount of time that passed on the GPU between starting and finishing the query

```java
//create query
GpuQuery query = new GpuQuery(GpuQuery.Type.SAMPLES_PASSED, renderer);
query.startQuery();
            
//now render some geometries
...
            
//stop the query
query.stopQuery();
            
//either: during next frame or some time later query the result
long samplesPassed = query.getResult();
//or : directly after stopping the query, store the result in a QueryBuffer
query.storeResult(buffer, offset, use64Bits, wait);
```

## CameraUniformBuffer

One usage of UniformBuffers has been build into the engine: a uniform buffer that contains all camera-related data of the scene, it has the following layout:
```glsl
layout (shared) uniform g_Camera {  
    mat4    cam_viewMatrix;
    mat4    cam_projectionMatrix;
    mat4    cam_viewProjectionMatrix;
    mat4    cam_viewMatrixInverse;
    mat4    cam_projectionMatrixInverse;
    mat4    cam_viewProjectionMatrixInverse;
    
    vec4    cam_rotation;
    
    vec3    cam_position;
    float   cam_height;
    vec3    cam_direction;
    float   cam_width;
    vec3    cam_left;
    float   cam_frustumTop;
    vec3    cam_up;
    float   cam_frustumBottom;
    
    float   cam_frustumLeft;
    float   cam_frustumRight;
    float   cam_frustumNear;
    float   cam_frustumFar;
    float   cam_viewPortLeft;
    float   cam_viewPortRight;
    float   cam_viewPortTop;
    float   cam_viewPortBottom; 
    
    float   cam_time;
    float   cam_tpf;  
};
```
And a new Material WorldParameter has been added: CameraBuffer. This means all camera related data has to be send to the GPU once only at the beginning of the frame and not once for each geometry that requires it. This does not include WorldMatrix or WorldViewMatrix etc ofc as those are different for each geometry. For a working example see https://github.com/AKasigkeit/jmonkeyengine/blob/master/jme3-examples/src/main/java/jme3test/buffers/TestCameraUniformBuffer.java and the Material at https://github.com/AKasigkeit/jmonkeyengine/tree/master/jme3-examples/src/main/resources/jme3test/ubo

## Other Changes

Several things have been changed under the hood that dont directly expose new features, most notable:
 - VAOs (VertexArrayObjects) have been added. Basically before rendering a mesh, all VertexBuffers and if used also the IndexBuffer have to be bound, additionally VertexAttributePointers have to be updated and while those calls might not be as heavyweight as drawcalls for example, they still require native calls and sending the calls from the client thread to the server thread where they even require some checks to make sure those calls are valid. While those states change between different geometries (they use different buffers for their vertex data, at least with the usual setup), those states usually dont change within the same geometry between 2 frames (it still uses the same vertex buffers next frame). Thus this state can be stored, and reused when the mesh is rendered next time. This requires OpenGL 3
 - Texture- and BufferBindings have been changed. When setting textures or buffers, the renderer reuses binding units in case the requested texture / buffer is already bound and during initialization the OpenGL implementation is queried for its specific maximum number of binding points for the specified type / target so more textures / buffers can be bound at the same time. (this is not noticable to the user, but it increases the chances that a texture / buffer is bound already when it is needed, for that reason i also recommend using TextureArrays whenever possible to pack several textures into one texture object) 

## Capabilities
new Capabilities like Caps.ComputeShader, Caps.BufferStorage, Caps.MultiDrawIndirect, Caps.ImageLoadStore, Caps.QueryBuffer or Caps.SyncObjects have been added to check what the hardware the program currently runs on supports

## Examples

Several examples were added under https://github.com/AKasigkeit/jmonkeyengine/tree/master/jme3-examples/src/main/java/jme3test/buffers and https://github.com/AKasigkeit/jmonkeyengine/tree/master/jme3-examples/src/main/java/jme3test/compute

 
Link to the original repo: https://github.com/jMonkeyEngine/jmonkeyengine/
