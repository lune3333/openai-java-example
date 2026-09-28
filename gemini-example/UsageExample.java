// Build your client using the custom Builder
LlmClient llmClient = LlmClient.builder()
        .apiKey("sk-YOUR-API-KEY")
        .apiUrl("https://api.your-custom-endpoint.com/v1/")
        .proxy("127.0.0.1", 8080)
        .certCaFilePath("/path/to/enterprise-ca.pem")
        .model("gpt-4o")
        .build();

// Fetch the native client from your wrapper
OpenAIClient openAiClient = llmClient.getOpenAIClient();

// Make the API Call utilizing your saved default model
ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
        .model(llmClient.getConfig().getModel())
        .addMessage(ChatCompletionUserMessageParam.builder()
                .content("Rate this joke: If you ignore your stakeholders concerns, your project is guaranteed to become an absolute requirement.")
                .build())
        .build();

openAiClient.chat().completions().create(params);