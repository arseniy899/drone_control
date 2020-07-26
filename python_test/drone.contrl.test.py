#!/usr/bin/env python

import asyncio
import websockets



import sys
if("show" in sys.argv):
	import numpy as np
	import cv2
import io
import subprocess as sp
import threading
import time
import json
from functools import partial
import shlex

# Build synthetic video and read binary data into memory (for testing):
#########################################################################
width, height = 640, 480
sp.run(shlex.split('ffmpeg -y -f lavfi -i testsrc=size={}x{}:rate=1 -vcodec vp9 -crf 23 -t 50 test.webm'.format(width, height)))

with open('test.webm', 'rb') as binary_file:
	in_bytes = binary_file.read()
#########################################################################


# https://stackoverflow.com/questions/5911362/pipe-large-amount-of-data-to-stdin-while-using-subprocess-popen/14026178
# https://stackoverflow.com/questions/15599639/what-is-the-perfect-counterpart-in-python-for-while-not-eof
# Write to stdin in chunks of 1024 bytes.
def writer():
	for chunk in iter(partial(stream.read, 1024), b''):
		process.stdin.write(chunk)
	try:
		process.stdin.close()
	except (BrokenPipeError):
		pass  # For unknown reason there is a Broken Pipe Error when executing FFprobe.


# Get resolution of video frames using FFprobe
# (in case resolution is know, skip this part):
################################################################################
# Open In-memory binary streams
stream = io.BytesIO(in_bytes)

process = sp.Popen(shlex.split('ffprobe -v error -i pipe: -select_streams v -print_format json -show_streams'), stdin=sp.PIPE, stdout=sp.PIPE, bufsize=10**8)

pthread = threading.Thread(target=writer)
pthread.start()

pthread.join()

in_bytes = process.stdout.read()

process.wait()

p = json.loads(in_bytes)

width = (p['streams'][0])['width']
height = (p['streams'][0])['height']
################################################################################


# Decoding the video using FFmpeg:
################################################################################
stream.seek(0)

# FFmpeg input PIPE: WebM encoded data as stream of bytes.
# FFmpeg output PIPE: decoded video frames in BGR format.
process = sp.Popen(shlex.split('ffmpeg -i pipe: -f rawvideo -pix_fmt bgr24 -an -sn pipe:'), stdin=sp.PIPE, stdout=sp.PIPE, bufsize=10**8)

thread = threading.Thread(target=writer)
thread.start()

frames = {}
frameCount = 0
# Read decoded video (frame by frame), and display each frame (using cv2.imshow)
while True:
	# Read raw video frame from stdout as bytes array.
	in_bytes = process.stdout.read(width * height * 3)
	
	if not in_bytes:
		break  # Break loop if no more bytes.
	frames[frameCount] = in_bytes
	frameCount += 1
	if("show" in sys.argv):
		# Transform the byte read into a NumPy array
		in_frame = (np.frombuffer(in_bytes, np.uint8).reshape([height, width, 3]))

		# Display the frame (for testing)
		cv2.imshow('in_frame', in_frame)

		if cv2.waitKey(100) & 0xFF == ord('q'):
			break
print("frames #",frameCount)
if not in_bytes:
	# Wait for thread to end only if not exit loop by pressing 'q'
	thread.join()

#try:
#	process.wait(1)
#except (sp.TimeoutExpired):
#	process.kill()  # In case 'q' is pressed.
################################################################################

#cv2.destroyAllWindows()
curFrame = -1
def getFrame():
	global curFrame
	global frames
	global frameCount
	if(curFrame > (frameCount-2)):
		curFrame = -1
	curFrame += 1
	return frames[curFrame]
client = None
async def echo(websocket, path):
	global client
	client = websocket
	async for message in websocket:
		#	#await websocket.send(message)
		#await websocket.send(getFrame())
		print("Recived", message)
	
	
		#await websocket.recv()

async def broadcast():
	while True:
		if(client != None):
			await client.send(getFrame())
			print("Sening frame #",curFrame)
		await asyncio.sleep(0.1)
asyncio.get_event_loop().run_until_complete(websockets.serve(echo, '', 8765))
print("Ready for webSocket")
while True:
	try:
		
		asyncio.get_event_loop().run_until_complete(broadcast())
		asyncio.get_event_loop().run_forever()
	except:
		#print("Closed")
		pass
