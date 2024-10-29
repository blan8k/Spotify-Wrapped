Due to possible security threats, we cannot release our own OpenAI API key and this unfortunately interferes with the AI feature of this app.
However, by following a few simple instructions, the user can incorporate their own API key and unlock this component of the app!

Steps to Enable the AI Feature in This App

1. Get Your OpenAI API Key.

Go over to OpenAI's website and create an account if you don’t have one already. Once you're set up, you can generate your API key.

https://auth.openai.com/authorize?issuer=auth0.openai.com&client_id=DRivsnm2Mu42T3KOpqdtwB3NYviHYzwD&audience=https%3A%2F%2Fapi.openai.com%2Fv1&redirect_uri=https%3A%2F%2Fplatform.openai.com%2Fauth%2Fcallback&device_id=0536b607-a0f3-4414-b3ae-db62b86c7e63&screen_hint=signup&max_age=0&scope=openid%20profile%20email%20offline_access&response_type=code&response_mode=query&state=YXNDalhLbVEzLXNlaEJJZE10d0RyMEVUaVJzSDQ5UXQ3dHRMRnBkS0hjNA%3D%3D&nonce=TFA4a0RXTEtVQ1doOVMtRXEtbWVmdGxrR1NxT0xnalZGc1F3WndRSTFJZw%3D%3D&code_challenge=gZVNEglLZZs0p_UVk_Oy-0yVqUX2Mf2HBPdAgrpVk4A&code_challenge_method=S256&auth0Client=eyJuYW1lIjoiYXV0aDAtc3BhLWpzIiwidmVyc2lvbiI6IjEuMjEuMCJ9

2. Create a local.properties File.
 
In the main directory of the project (where the app folder is located), create a file named local.properties if it doesn’t already exist.

3. Add Your API Key.
 
Open the local.properties file and add this line:
OPENAI_API_KEY="your_actual_api_key_here"

Replace "your_actual_api_key_here" with your actual API key from OpenAI.

4. Sync and Rebuild the Project.

In Android Studio, go to File > Sync Project with Gradle Files to refresh the settings. Once synced, go to Build > Clean Project and then Build > Rebuild Project.
That’s it! Your AI feature should now be ready to go.
Have fun!
