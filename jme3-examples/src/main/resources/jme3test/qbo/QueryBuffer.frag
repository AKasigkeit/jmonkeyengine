
uniform sampler2D m_ColorMap;
uniform int m_QueryIndex;

layout (std430) buffer m_QueryResults {
    uint samplesPassed[NUM_QUERIES];
};

in vec2 texCoord;

out vec4 outFragColor;

void main(void) {
    uint samples = samplesPassed[m_QueryIndex];
    float factor = clamp(float(samples) / 1000.0, 0.0, 1.0);
    outFragColor = vec4(float(samples) / float(NUM_QUERIES));// texture(m_ColorMap, texCoord) * factor;
}
