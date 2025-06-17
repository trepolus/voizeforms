package com.voizeforms.templates

import com.voizeforms.model.UserSession

object HtmlTemplates {
    
    fun loginPage(errorMessage: String = ""): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>VoizeForms - Login</title>
            <style>
                body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
                .login-btn { 
                    background: #4285f4; color: white; padding: 12px 24px; 
                    text-decoration: none; border-radius: 4px; display: inline-block;
                    margin: 20px 0;
                }
                .login-btn:hover { background: #357ae8; }
            </style>
        </head>
        <body>
            <h1>🎤 VoizeForms</h1>
            <p>Real-time Voice-to-Text Form Assistant</p>
            <a href="/auth/login" class="login-btn">🔐 Login with Google</a>
            $errorMessage
        </body>
        </html>
    """.trimIndent()
    
    fun dashboardPage(session: UserSession, baseUrl: String): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>VoizeForms - Voice Note Dashboard</title>
            <style>
                body { font-family: Arial, sans-serif; padding: 20px; max-width: 1000px; margin: 0 auto; }
                .user-info { background: #d4edda; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
                .dashboard-section { margin: 30px 0; padding: 20px; background: #f8f9fa; border-radius: 8px; }
                .button { 
                    background: #4285f4; color: white; padding: 10px 20px; 
                    border: none; border-radius: 4px; cursor: pointer; margin: 5px;
                }
                .button:hover { background: #357ae8; }
                .button:disabled { background: #ccc; cursor: not-allowed; }
                .danger { background: #dc3545; }
                .danger:hover { background: #c82333; }
                .success { background: #28a745; }
                .text-input { width: 100%; padding: 10px; margin: 10px 0; border: 1px solid #ccc; border-radius: 4px; }
                .transcription-output { 
                    background: #fff; border: 1px solid #ddd; padding: 15px; 
                    border-radius: 4px; min-height: 100px; margin: 10px 0;
                    font-family: monospace; white-space: pre-wrap;
                }
                .history-item { 
                    background: #fff; border: 1px solid #ddd; padding: 10px; 
                    margin: 5px 0; border-radius: 4px;
                }
                code { background: #e9ecef; padding: 2px 4px; border-radius: 3px; }
            </style>
        </head>
        <body>
            <h1>🎤 VoizeForms Dashboard</h1>
            <div class="user-info">
                <strong>✅ Authenticated as:</strong> ${session.name} (${session.email})
                <a href="/auth/logout" style="float: right; background: #dc3545; color: white; padding: 5px 10px; text-decoration: none; border-radius: 3px;">Logout</a>
            </div>
            
            <div class="dashboard-section">
                <h2>🎙️ Voice Note Recording</h2>
                <p>Create a new voice note and see real-time transcription:</p>
                
                <div>
                    <button id="startBtn" class="button">Start Recording</button>
                    <button id="stopBtn" class="button danger" disabled>Stop Recording</button>
                    <span id="status">Ready to record</span>
                </div>
                
                <div>
                    <h3>Simulate Speech Input:</h3>
                    <input type="text" id="speechInput" class="text-input" placeholder="Type your speech here and press Enter to simulate audio chunks..." disabled>
                </div>
                
                <div>
                    <h3>Live Transcription:</h3>
                    <div id="transcriptionOutput" class="transcription-output">No active session</div>
                </div>
            </div>
            
            <div class="dashboard-section">
                <h2>📚 Your Voice Notes History</h2>
                <button id="refreshHistory" class="button">Refresh History</button>
                <div id="historyContainer">Loading...</div>
            </div>
            
            <div class="dashboard-section">
                <h2>🔧 API Testing</h2>
                <p>Test the API endpoints directly:</p>
                <code>curl -X POST $baseUrl/api/v1/transcription/session</code><br>
                <code>curl -X POST $baseUrl/api/v1/transcription/session/{sessionId}/chunk --data-binary @audio.wav</code><br>
                <code>curl -X GET $baseUrl/api/v1/transcription/stream/{sessionId}</code>
            </div>

            <script>
                let currentSessionId = null;
                let eventSource = null;
                
                const startBtn = document.getElementById('startBtn');
                const stopBtn = document.getElementById('stopBtn');
                const status = document.getElementById('status');
                const speechInput = document.getElementById('speechInput');
                const transcriptionOutput = document.getElementById('transcriptionOutput');
                const historyContainer = document.getElementById('historyContainer');
                
                // Start recording session
                startBtn.addEventListener('click', async () => {
                    try {
                        const response = await fetch('/api/v1/transcription/session', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            credentials: 'include'
                        });
                        
                        if (response.ok) {
                            const data = await response.json();
                            currentSessionId = data.sessionId;
                            startBtn.disabled = true;
                            stopBtn.disabled = false;
                            speechInput.disabled = false;
                            status.textContent = `Recording - Session: ${'$'}{currentSessionId}`;
                            transcriptionOutput.textContent = '';
                            
                            // Start SSE connection  
                            eventSource = new EventSource(`/api/v1/transcription/stream/${'$'}{currentSessionId}`, {
                                withCredentials: true
                            });
                            eventSource.onmessage = (event) => {
                                const result = JSON.parse(event.data);
                                transcriptionOutput.textContent += result.text + ' ';
                            };
                            eventSource.onerror = () => {
                                console.error('SSE connection error');
                            };
                        } else {
                            alert('Failed to start session');
                        }
                    } catch (error) {
                        console.error('Error starting session:', error);
                        alert('Error starting session');
                    }
                });
                
                // Stop recording session
                stopBtn.addEventListener('click', async () => {
                    if (currentSessionId && eventSource) {
                        try {
                            const response = await fetch(`/api/v1/transcription/session/${'$'}{currentSessionId}`, {
                                method: 'DELETE',
                                credentials: 'include'
                            });
                            
                            if (response.ok) {
                                const data = await response.json();
                                alert(`Session saved with ID: ${'$'}{data.savedId || 'unknown'}`);
                            }
                            
                            eventSource.close();
                            eventSource = null;
                            currentSessionId = null;
                            startBtn.disabled = false;
                            stopBtn.disabled = true;
                            speechInput.disabled = true;
                            speechInput.value = '';
                            status.textContent = 'Ready to record';
                            refreshHistory();
                        } catch (error) {
                            console.error('Error stopping session:', error);
                        }
                    }
                });
                
                // Simulate speech input
                speechInput.addEventListener('keypress', async (e) => {
                    if (e.key === 'Enter' && currentSessionId && speechInput.value.trim()) {
                        try {
                            const response = await fetch(`/api/v1/transcription/session/${'$'}{currentSessionId}/chunk`, {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/octet-stream' },
                                body: new TextEncoder().encode(speechInput.value.trim()),
                                credentials: 'include'
                            });
                            
                            if (response.ok) {
                                speechInput.value = '';
                            }
                        } catch (error) {
                            console.error('Error sending chunk:', error);
                        }
                    }
                });
                
                // Load history
                async function refreshHistory() {
                    try {
                        const response = await fetch('/api/v1/transcription/history', {
                            credentials: 'include'
                        });
                        const history = await response.json();
                        
                        if (Array.isArray(history) && history.length > 0) {
                            historyContainer.innerHTML = history.map(item => 
                                `<div class="history-item">${'$'}{JSON.stringify(item)}</div>`
                            ).join('');
                        } else {
                            historyContainer.innerHTML = '<p>No voice notes yet. Start recording to create your first note!</p>';
                        }
                    } catch (error) {
                        historyContainer.innerHTML = '<p>Error loading history</p>';
                        console.error('Error loading history:', error);
                    }
                }
                
                document.getElementById('refreshHistory').addEventListener('click', refreshHistory);
                
                // Load initial history
                refreshHistory();
            </script>
        </body>
        </html>
    """.trimIndent()
} 