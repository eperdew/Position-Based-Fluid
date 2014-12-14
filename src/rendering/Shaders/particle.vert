#version 400

in vec3 vertexPos;
in vec3 color;

uniform mat4 projection;
uniform mat4 mView;
uniform vec2 screenSize;
uniform vec3 lightPos;

out vec3 pos;
out float radius;
out vec3 fragColor;

void main() {
	vec4 viewPos = mView * vec4(vertexPos, 1.0);
    float dist = length(viewPos);    
    gl_Position = projection * viewPos;
    gl_PointSize = screenSize.y / (8.0 * gl_Position.z); //fixme
    
    pos = viewPos.xyz;
    radius = gl_PointSize;
    fragColor = color;
}