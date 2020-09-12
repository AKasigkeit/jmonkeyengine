This fork attempts to bring more modern OpenGL (OpenGL4+) features to jMonkeyEngine.

## Most important changes:
- Brings ComputeShaders to jme3
- Makes use of VAOs to reduce glVertexAttrib and glBindBuffer calls drastically
- Keeps track of currently bound textures / buffers in a different way in an attempt to reduce related state switches
- Including using implementation dependant maximum for VertexBufferBindings, TextureBindings etc to further reduce switches as much as the driver allows
- OpenGL query objects have been implemented (or, well, reworked as the DetailedProfilerState made use of them already)
- Supports MultiDrawIndirect for DrawElementsIndirectCommands and DrawArraysIndirectCommands
- Buffers have been reworked. now supported buffer types:
  VertexBuffer (VBO), UniformBuffer (UBO), ShaderStorageBuffer (SSBO), AtomicCounterBuffer, DispatchIndirectBuffer, DrawIndirectBuffer and QueryBuffer

This fork is under active development and implementations as well as method signatures etc can change at any time.
Also, while i use it heavily myself i probably didnt yet come to try all the different usages, thus i expect bugs to exist

### Compute Shaders:
see https://github.com/AKasigkeit/jmonkeyengine/blob/master/jme3-examples/src/main/java/jme3test/compute/ComputeShaderTestInversion.java for an example that inverts a textures color and writes it into another texture bound as image


To make use of them, you first have to create a ComputeShaderFactory:
```java
ComputeShaderFactory factory = ComputeShaderFactory.create(renderer);
```
then you can instanciate ComputeShaders using a .comp file or a String as source:
```java
ComputeShader someShader    = factory.createComputeShader("Shaders/SomeShader.comp", "GLSL430");
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
When using the setTexture() method, the provided texture will be bound as texture and can be used in the shader
as always, this can make use of lods (although textureLod() needs to be used as only FragmentShaders can do automatic TextureLod calculation), MinFilter and MagFilter and no Format has to be specified explicity as the sampler will 
handle all of that.
However when using setImage(), the provided texture will be bound as image and can be accessed via imageLoad() and imageStore(), but only 1 mipmap level can be bound, the format has to be explicity stated (and much less formats are supported) but it enabled writes to the data. A setImage call can look like that:
```java
int mipmapLevel = 0;       //bind max size mipmap level
int layers = -1;           //-1 means bind all layers (in case of TextureArray, TextureCubeMap or Texture3D)
boolean useDefines = true; //sets 3 defines in the shader: NAME_WIDTH, NAME_HEIGHT and NAME_FORMAT
someShader.setImage("Input", VarType.Texture2D, tex, Access.ReadOnly, mipmapLevel, layers, useDefines);
```

## Buffers:
see https://github.com/AKasigkeit/jmonkeyengine/blob/master/jme3-examples/src/main/java/jme3test/buffers for some examples

all buffers have static factory methods like:
```java
UniformBuffer ubo            = UniformBuffer.createNewEmpty();
AtomicCounterBuffer acbo     = AtomicCounterBuffer.createWithInitialValues(0, 0);
QueryBuffer qbo              = QueryBuffer.createWithSize(16, true);
ShaderStorageBuffer ssbo     = ShaderStorageBuffer.createNewEmpty();
DrawIndirectBuffer dribo     = DrawIndirectBuffer.createWithCommands(commands);
DispatchIndirectBuffer diibo = DispatchIndirectBuffer.createWithCommand(command);
```
However the actual intention behind the buffer rework was to allow one buffer to be used as different targets. For this the UntypedBuffer has been introduced. It represents a raw buffer on the GPU that does not have any specific type. It can be created in MemoryMode.CpuGpu (data will be kept on the CPU) or MemoryMode.GpuOnly (no data will be kept on the CPU), can be used in Direct or Lazy mode (In Direct mode method calls that change the data will directly send the related update to the GPU as well, while Lazy mode will first send data once it is actually needed for rendering or in a compute shader) and it can use the StorageAPI.BufferData as well as StorageAPI.Storage (the first one is supported on all OpenGL versions that jme supports, while the StorageAPI.Storage is only available on OpenGL3 but is required for persistenly mapped buffers and can allow for more optimizations) 
Lazy mode is jme's default behaviour, however Direct mode allows for data upload in batches. For example an UntypedBuffer in MemoryMode.GpuOnly acting in DirectMode can allocate 128MB of data on the GPU side, while on the CPU side you never need to hold that much data but can reuse say a 2MB buffer to upload data in batches or to only update a small portion of the buffer without having to send all the data again)
Downside of the Lazy Mode: only one update / download can be used as they will not be queued, meaning when a data download from the GPU is requested say after a ComputeShader wrote some data, the data will not be read back immediately, instead it will first be read when the buffer is used for rendering or in a compute shader next time, it will however be read from the GPU before the actual draw or dispatch commands will be made.

An example of how to create an UntypedBuffer and 2 views on it:
```java
UntypedBuffer buffer = UntypedBuffer.createNewBufferDataDirect(MemoryMode.GpuOnly, renderer, BufferDataUsage.StaticDraw);
buffer.initialize(someSize);
ShaderStorageBuffer ssbo = buffer.asShaderStorageBuffer(null);
VertexBuffer posBuffer = buffer.asVertexBuffer(Type.Position, Format.Float, 3, 12, 0); //last params are stride and offset
```
then the ShaderStorageBuffer ssbo can be bound to a ComputeShader to write data to it, while the VertexBuffer can be set on a Mesh to use the data as inPosition in the VertexShader (have to take into account the layout for ssbo). Useful for many things including particle systems etc
following that idea, an UntypedBuffer could also be viewed as ShaderStorageBuffer and DrawIndirectBuffer at the same time to let a ComputeShader do frustum culling and fill the data of the DrawIndirectBuffer via its ShaderStorageBuffer-view.
Also an UntypedBuffer can be viewed as ShaderStorageBuffer and QueryBuffer to make the result of the query available to the ComputeShader without reading it back to the CPU (tessellation levels could be reduced when the mesh is occluded, lower resolution texture mipmaps can be used or less performance intense light calculations etc you get the point)

A RingBuffer interface has been added with the SingleBufferRingBuffer and MultiBufferRingBuffer implementations that use Persistently Mapped Buffers to stream data to the GPU while providing synchronization and fencing out of the box to make sure you dont write data to a block of the buffer that the GPU currently reads from

## Capabilities
new Capabilities like Caps.ComputeShader, Caps.BufferStorage, Caps.MultiDrawIndirect, Caps.ImageLoadStore, Caps.QueryBuffer or Caps.SyncObjects have been added to check what the hardware the program currently runs on supports




Link to the original repo: https://github.com/jMonkeyEngine/jmonkeyengine/
