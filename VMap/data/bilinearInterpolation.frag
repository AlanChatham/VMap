// bilinearInterpolation.frag
#version 330

uniform mat4 transform;
uniform sampler2D tex;

in vec4 vertColor;
in vec3 texCoord;

in vec2 q;
in vec2 b1;
in vec2 b2;
in vec2 b3;

out vec4 fragColor;

float Wedge2D(vec2 v, vec2 w){
  return v.x*w.y - v.y*w.x;
}

void main() {
  // Set up quadratic formula
  float A = Wedge2D(b2, b3);
  float B = Wedge2D(b3, q) - Wedge2D(b1, b2);
  float C = Wedge2D(b1, q);
  
  // Solve for v
  vec2 uv;
  if (abs(A) < 0.001){
    // Linear form
    uv.y = -C/B;
  }
  else{
    //Quadratic form - take positive root for CCW winding with V-up
    float discrim = B*B - 4*A*C;
    uv.y = 0.5 * (-B + sqrt(discrim)) / A;
  }
  
  // Solve for u, using largest-magnitude component
  vec2 denom = b1 + uv.y * b3;
  if (abs(denom.x) > abs(denom.y))
    uv.x = (q.x - b2.x * uv.y) / denom.x;
  else
    uv.x = (q.y - b2.y * uv.y) / denom.y;
  
  fragColor = texture(tex, uv) * vertColor;
  
  //fragColor = texture(tex, texCoord.xy) * vertColor;
  //fragColor = vec4(1.0f, .8f, 1.0f, 1.0f);
}