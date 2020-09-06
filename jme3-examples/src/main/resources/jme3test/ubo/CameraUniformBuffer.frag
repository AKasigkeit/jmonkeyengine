uniform sampler2D m_ColorMap;

in vec2 texCoord;

out vec4 outFragColor;

void main(void) {
    outFragColor = texture(m_ColorMap, texCoord);
}
