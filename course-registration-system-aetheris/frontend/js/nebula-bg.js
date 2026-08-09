// =====================================================================
// Aetheris nebula background — a single reusable WebGL layer.
// Adapted from the Stitch mockup's login-page shader so every page gets
// the same animated background without duplicating ~150 lines of WebGL
// setup per file. Injects its own <canvas id="glcanvas"> if one isn't
// already on the page, so it's a one-line include: <script src="js/nebula-bg.js"></script>
// =====================================================================

(function initNebulaBackground() {
  let canvas = document.getElementById('glcanvas');
  if (!canvas) {
    canvas = document.createElement('canvas');
    canvas.id = 'glcanvas';
    document.body.insertBefore(canvas, document.body.firstChild);
  }

  const gl = canvas.getContext('webgl');
  if (!gl) return; // Graceful no-op on old browsers — page still works, just without the animated backdrop.

  const vertexShaderSource = `
    attribute vec2 a_position;
    varying vec2 v_texCoord;
    void main() {
      gl_Position = vec4(a_position, 0.0, 1.0);
      v_texCoord = a_position * 0.5 + 0.5;
    }
  `;

  const fragmentShaderSource = `
    precision highp float;
    uniform float u_time;
    uniform vec2 u_resolution;
    varying vec2 v_texCoord;

    float noise(vec2 p) {
      return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
    }

    void main() {
      vec2 uv = v_texCoord;

      // Deep, evolving gradient between near-black surface and electric cyan
      vec3 color1 = vec3(0.043, 0.055, 0.082); // #0b0e15
      vec3 color2 = vec3(0.0, 0.855, 0.973);   // #00daf8

      float wave = sin(uv.x * 3.0 + u_time * 0.5) * 0.5 + 0.5;
      wave += cos(uv.y * 2.0 - u_time * 0.3) * 0.3;

      // Slow-moving nebula clouds
      float clouds = 0.0;
      vec2 shift = vec2(u_time * 0.02);
      clouds += noise(uv * 3.0 + shift) * 0.1;
      clouds += noise(uv * 6.0 - shift) * 0.05;

      vec3 finalColor = mix(color1, color2 * 0.08, wave + clouds);

      // Vignette so the edges stay dark and content stays legible
      float vignette = 1.0 - smoothstep(0.5, 1.5, length(uv - 0.5));
      finalColor *= vignette;

      gl_FragColor = vec4(finalColor, 1.0);
    }
  `;

  function createShader(type, source) {
    const shader = gl.createShader(type);
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
      console.error('Nebula background: shader compile error', gl.getShaderInfoLog(shader));
      gl.deleteShader(shader);
      return null;
    }
    return shader;
  }

  const vertexShader = createShader(gl.VERTEX_SHADER, vertexShaderSource);
  const fragmentShader = createShader(gl.FRAGMENT_SHADER, fragmentShaderSource);
  if (!vertexShader || !fragmentShader) return;

  const program = gl.createProgram();
  gl.attachShader(program, vertexShader);
  gl.attachShader(program, fragmentShader);
  gl.linkProgram(program);
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
    console.error('Nebula background: program link error', gl.getProgramInfoLog(program));
    return;
  }

  const positionBuffer = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, 1, 1, 1, -1, -1, 1, -1]), gl.STATIC_DRAW);

  const positionLocation = gl.getAttribLocation(program, 'a_position');
  const timeLocation = gl.getUniformLocation(program, 'u_time');
  const resolutionLocation = gl.getUniformLocation(program, 'u_resolution');

  // Pause the animation loop when the tab isn't visible — a real cost-saver
  // for a background effect running on every single page, not just one.
  let running = true;
  document.addEventListener('visibilitychange', () => {
    running = !document.hidden;
    if (running) requestAnimationFrame(render);
  });

  function render(timeMs) {
    if (!running) return;
    const time = timeMs * 0.001;

    if (canvas.width !== window.innerWidth || canvas.height !== window.innerHeight) {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
      gl.viewport(0, 0, canvas.width, canvas.height);
    }

    gl.useProgram(program);
    gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
    gl.enableVertexAttribArray(positionLocation);
    gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0);
    gl.uniform1f(timeLocation, time);
    gl.uniform2f(resolutionLocation, canvas.width, canvas.height);
    gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);

    requestAnimationFrame(render);
  }

  requestAnimationFrame(render);
})();
