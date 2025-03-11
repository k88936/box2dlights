#version 330 core
attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
attribute vec2 a_binormal;
uniform mat4 u_projTrans;
varying vec4 v_color;
varying mat2 v_rot;
varying vec2 v_texCoords;

void main()
{
    v_rot = mat2( a_binormal.x, a_binormal.y,
    -a_binormal.y, a_binormal.x);
    v_color = a_color;
    v_color.a = v_color.a * (255.0/254.0);
    v_texCoords = a_texCoord0;
    gl_Position =  u_projTrans * a_position;
}
