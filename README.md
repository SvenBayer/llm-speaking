# LLM Speaking - DEMO
This is part of the LLM friend chatbot demo and uses the Evolving GraphRAG concept.

**DISCLAIMER:** This is a demo and not a production-ready solution. The code was created late at night and is provided as-is and should be used with caution.

## Architecture
![LLM Friend Chatbot Architecture](https://svenbayer.wordpress.com/wp-content/uploads/2025/04/image-3.png)
The LLM Friend Chatbot consists of 8 services.

**LLM Speaking Service** uses a Whisper Docker container to transcribe the audio from the user to text and uses Zonos in a Docker container to create an audio responds to play the answer.

**TTS Zonos** is a Spring Boot service that is just used to start the Zonos Docker container and generate audios with specific emotion settings.

**LLM Friend Chatbot** is the core service which creates the final answer with the LLM.

**LLM Friend Chat History** summarizes the conversation history to allow better performance/answers from the LLM.

**LLM Friend Memory Organizer** uses Neo4j in a Docker container and extracts Memory entities, creates Summary and Assumption entities and extracts relevant information via vector search and graph search, so GraphRAG. As special feature, it contains the database password in plain in application.yml.

![LLM Interaction Cycle](https://svenbayer.wordpress.com/wp-content/uploads/2025/04/image-4.png)

## References
Read my blogpost: [Blogpost about Evolving GraphRAG](https://svenbayer.wordpress.com/2025/04/10/evolving-graphrag-with-genai/) to get a better understanding.