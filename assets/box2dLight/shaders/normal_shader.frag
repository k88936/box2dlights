#version 330 core
#define LOWP lowp
precision mediump float;
varying mat2 v_rot;
varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
void main()
{
    vec4 normal = texture2D(u_texture, v_texCoords).rgba;
    vec2 scaledNormal = normal.xy * 2.0 - 1.0;
    vec2 rotated = vec2(v_rot * scaledNormal);
    rotated = (rotated.xy / 2.0 + 0.5);
    gl_FragColor = vec4(rotated.xy, normal.z, normal.a);
}