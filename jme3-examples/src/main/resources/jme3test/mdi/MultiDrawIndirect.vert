#import "Common/ShaderLib/CameraUniformBuffer.glsllib"
 
in vec3 inTranslation;
in vec3 inPosition;
in vec3 inColor; 

out vec3 color;

void main(void) {
    color = inColor;
    vec4 wsPos = vec4(inTranslation + inPosition, 1.0);
    gl_Position = cam_viewProjectionMatrix * wsPos;
}