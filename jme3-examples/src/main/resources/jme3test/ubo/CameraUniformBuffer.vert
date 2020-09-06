#import "Common/ShaderLib/CameraUniformBuffer.glsllib"

uniform mat4 g_WorldMatrix;

in vec3 inPosition;
in vec2 inTexCoord; 

out vec2 texCoord;

void main(void) {
    texCoord = inTexCoord;
    vec4 wsPos = g_WorldMatrix * vec4(inPosition, 1.0);
    gl_Position = cam_viewProjectionMatrix * wsPos;
}