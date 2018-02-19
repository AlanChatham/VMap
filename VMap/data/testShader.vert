// vert.glsl
#version 330

in vec3 position;
in vec4 color;
in vec3 texLoc;

out vec4 vertColor;
out vec3 texCoord;

void main() {
  gl_Position = vec4(position.x, position.y, position.z, 1.0f);
  
  // Color is coming out with RGB swapped right now, so see if this fixes...
  vertColor = color;
  
  texCoord = texLoc;
}