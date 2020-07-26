#!/bin/bash
ROOT=$PWD
cat > /lib/systemd/system/DC_test.service  <<EOF
[Unit]
Description=Test WebSocket server for Drone Controller
After=multi-user.target
 
[Service]
Type=simple
ExecStart=/usr/bin/python3 /root/drone.contrl.test.py
Restart=always
RestartSec=3
 
[Install]
WantedBy=multi-user.target
EOF

chmod 644 /lib/systemd/system/DC_test.service
systemctl daemon-reload
systemctl enable DC_test.service
systemctl start DC_test.service