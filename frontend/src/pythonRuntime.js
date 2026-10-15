let pyodidePromise = null
let interruptBuffer = null

export async function loadPyodideRuntime() {
  if (!window.loadPyodide) {
    await new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = 'https://cdn.jsdelivr.net/pyodide/v0.26.4/full/pyodide.js'
      script.onload = resolve
      script.onerror = () => reject(new Error('Could not load Pyodide.'))
      document.head.appendChild(script)
    })
  }
  if (!pyodidePromise) {
    pyodidePromise = window.loadPyodide({
      indexURL: 'https://cdn.jsdelivr.net/pyodide/v0.26.4/full/',
    })
  }
  const pyodide = await pyodidePromise
  if (!interruptBuffer && typeof SharedArrayBuffer !== 'undefined') {
    interruptBuffer = new Uint8Array(new SharedArrayBuffer(1))
    pyodide.setInterruptBuffer(interruptBuffer)
  }
  return pyodide
}

export function requestStop() {
  if (interruptBuffer) {
    interruptBuffer[0] = 2
  }
}

export function clearStop() {
  if (interruptBuffer) {
    interruptBuffer[0] = 0
  }
}
