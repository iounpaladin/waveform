import numpy as np
import matplotlib.pyplot as plt

# Adjust these to match your data
SAMPLE_RATE = 44100
DTYPE = np.int16      # 16-bit signed little-endian
CHANNELS = 2

# Load raw bytes
data = np.fromfile("../after.bin", dtype=DTYPE)

# If stereo, reshape to (frames, channels)
if CHANNELS > 1:
    data = data.reshape(-1, CHANNELS)

# Build a time axis in seconds
t = np.arange(len(data)) / SAMPLE_RATE

plt.figure(figsize=(12, 4))
plt.plot(t, data, linewidth=0.5)
plt.xlabel("Time (s)")
plt.ylabel("Amplitude")
plt.title("Waveform")
plt.tight_layout()
plt.show()
