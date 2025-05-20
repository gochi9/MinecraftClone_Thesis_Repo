#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoord;
layout(location = 2) in float brightness;
layout(location = 3) in vec3 tintColor;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

const vec3 lightDir = normalize(vec3(-1.0, -1.0, -0.5));

out vec2 fragTexCoord;
out float fragBrightness;
out vec3 fragTintColor;

void main() {
    gl_Position = projectionMatrix * viewMatrix * vec4(position, 1.0);
    fragTexCoord = texCoord;

    fragBrightness = brightness;
    fragTintColor = tintColor;
}
