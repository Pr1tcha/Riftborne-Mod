layout(location = 0) in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 SkyRotation;

out vec3 skyDirection;

void main() {
    vec3 viewDirection = normalize((ModelViewMat * vec4(Position, 0.0)).xyz);
    skyDirection = normalize((SkyRotation * vec4(viewDirection, 0.0)).xyz);
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
