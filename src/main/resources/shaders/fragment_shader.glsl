#version 330 core

in vec2 fragTexCoord;
in float fragBrightness;
in vec3 fragTintColor;

out vec4 FragColor;

uniform sampler2D textureSampler;

void main() {
    vec4 texColor = texture(textureSampler, fragTexCoord);

    float brightness = clamp(fragBrightness, 0.0, 1.0);
    texColor.rgb *= mix(0.4, 0.9, brightness); // tone down lighting

    texColor.rgb *= mix(vec3(0.8), fragTintColor, 0.8);

    if (texColor.a < 0.1)
        discard;

    FragColor = texColor;
}
