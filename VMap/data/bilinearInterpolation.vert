// bilinearInterpolation.vert
#version 330

uniform mat4 transform;
uniform vec2 p0;
uniform vec2 p1;
uniform vec2 p2;
uniform vec2 p3;

//in vec4 position;
in vec3 position;
in vec3 color;
in vec3 texLoc;


out vec4 vertColor;
out vec3 texCoord;

out vec2 q;
out vec2 b1;
out vec2 b2;
out vec2 b3;

void main() {
  // Position
  gl_Position = vec4(position.x, position.y, position.z, 1.0f);
  // Color
  vertColor = vec4(color, 1.0f);
  
  // Texture position, likely unused
  texCoord = texLoc;
  
  // Set up for inverse bilinear interpolation!
  q = position.xy - p0;
  b1 = p1 - p0;
  b2 = p2 - p0;
  b3 = p0 - p1 - p2 + p3;
}