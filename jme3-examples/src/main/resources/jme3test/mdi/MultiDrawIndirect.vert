#import "Common/ShaderLib/CameraUniformBuffer.glsllib"
 
in vec3 inPosition;
in vec3 inColor; 
in vec3 inTranslation;
#ifdef SCALE
in vec3 inScale;
#endif

out vec3 color;

void main(void) {
    color = inColor;
    vec4 wsPos;
    #ifdef SCALE
        wsPos = vec4(inPosition * inScale, 1.0);
    #else
        wsPos = vec4(inPosition, 1.0);
    #endif
    wsPos = vec4(inTranslation + wsPos.xyz, 1.0);

    gl_Position = cam_viewProjectionMatrix * wsPos;
}