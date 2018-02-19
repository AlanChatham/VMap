// frag.glsl
#version 330

uniform mat4 transform;
uniform sampler2D tex;

in vec4 vertColor;
in vec3 texCoord;

out vec4 fragColor;


void main() {
  float q = texCoord.z;
  fragColor = texture(tex, texCoord.xy/q) * vertColor;
  
  //fragColor = vec4(1.0f, 1.0f, 0.6f, 1.0f);//q/2.0f);
}