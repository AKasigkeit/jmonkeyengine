#import "Common/ShaderLib/CameraUniformBuffer.glsllib"
 

in float inInstanceID;
in vec3 inPosition;
in vec2 inTexCoord; 
#ifdef WORLD_MATRIX
in mat4 inWorldMatrix;
#endif

out vec2 texCoord;

void main(void) {
    texCoord = inTexCoord;
    #ifdef WORLD_MATRIX
    vec4 wsPos = inWorldMatrix * vec4(inPosition, 1.0);
    #else
    vec4 wsPos = vec4(inPosition, 1.0) + vec4(mod(inInstanceID, 100.0) * 1.1, (inInstanceID / 100.0) * 1.1, 0.0, 0.0);
    #endif
    gl_Position = cam_viewProjectionMatrix * wsPos;
}