const path = require('path');
const fs = require('fs');
const { execFile } = require('child_process');
const { promisify } = require('util');
const AppError = require('./AppError');

const execFileAsync = promisify(execFile);

// backend/src/utils -> up 3 levels -> project root -> ml/
const ML_DIR = path.join(__dirname, '..', '..', '..', 'ml');

// Picks whichever venv layout actually exists (Windows vs. Mac/Linux use
// different folder names inside a venv) so this works for every teammate
// regardless of OS, falling back to a bare 'python3' on PATH as a last
// resort if no venv was ever created.
function resolvePythonPath() {
  const windowsVenvPython = path.join(ML_DIR, '.venv', 'Scripts', 'python.exe');
  const posixVenvPython = path.join(ML_DIR, '.venv', 'bin', 'python');
  if (fs.existsSync(windowsVenvPython)) return windowsVenvPython;
  if (fs.existsSync(posixVenvPython)) return posixVenvPython;
  return 'python3';
}

// Runs a script from ml/ as a one-off child process rather than standing
// up a separate always-running ML service — simpler to operate for a
// college mini-project (one server to start, not two). Returns the
// script's stdout so the caller can surface it for transparency.
async function runMlScript(scriptFilename) {
  const scriptPath = path.join(ML_DIR, scriptFilename);
  try {
    const { stdout } = await execFileAsync(resolvePythonPath(), [scriptPath], { cwd: ML_DIR });
    return stdout.trim();
  } catch (err) {
    throw new AppError(500, `${scriptFilename} failed: ${err.stderr || err.message}`);
  }
}

module.exports = { runMlScript };
