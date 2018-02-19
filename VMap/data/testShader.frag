#version 330

uniform mat4 transform;
uniform sampler2D tex;

in vec4 vertColor;
in vec3 texCoord;

out vec4 fragColor;


void main() {
  float q = texCoord.z;
  fragColor = texture(tex, texCoord.xy) * vertColor;
  
}