// vert.glsl
#version 330

uniform mat4 transform;

in vec3 position;
in vec3 color;
in vec3 texLoc;

out vec4 vertColor;
out vec3 texCoord;

void main() {
  //gl_Position = transform * position;
  gl_Position = vec4(position.x, position.y, position.z, 1.0f);
  //vertColor = vec4(.5f, 0.0f, 0.0f, 1.0f);
  vertColor = vec4(color, 1.0f);
  
  texCoord = texLoc;
}