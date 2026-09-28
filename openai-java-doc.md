# OPENAI JAVA DOC IN MD

[Doc link](https://developers.openai.com/api/docs?site_locale=en)

## Dependency
```xml
<dependency>
  <groupId>com.openai</groupId>
  <artifactId>openai-java</artifactId>
  <version>4.70.0</version>
</dependency>
```

## Basic API request
```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

public class Main {
  public static void main(String[] args) {
    OpenAIClient client = OpenAIOkHttpClient.fromEnv();

    ResponseCreateParams params =
        ResponseCreateParams.builder().input("Say this is a test").model("gpt-6-astra").build();

    Response response = client.responses().create(params);
    response.output().stream()
        .flatMap(item -> item.message().stream())
        .flatMap(message -> message.content().stream())
        .flatMap(content -> content.outputText().stream())
        .forEach(outputText -> System.out.println(outputText.text()));
  }
}
```

## Conversation state

Learn how to manage conversation state during a model interaction.

### Manually manage conversation state

While each text generation request is independent and stateless, you can still implement multi-turn conversations by providing additional messages as parameters to your text generation request. Consider a knock-knock joke:

```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputItem;
import java.util.List;

ResponseCreateParams params =
    ResponseCreateParams.builder()
        .model("gpt-6-astra")
        .inputOfResponse(
            List.of(
                ResponseInputItem.ofEasyInputMessage(
                    EasyInputMessage.builder()
                        .role(EasyInputMessage.Role.USER)
                        .content("Knock knock.")
                        .build()),
                ResponseInputItem.ofEasyInputMessage(
                    EasyInputMessage.builder()
                        .role(EasyInputMessage.Role.ASSISTANT)
                        .content("Who's there?")
                        .build()),
                ResponseInputItem.ofEasyInputMessage(
                    EasyInputMessage.builder()
                        .role(EasyInputMessage.Role.USER)
                        .content("Orange.")
                        .build())))
        .build();

client.responses().create(params).output().stream()
    .flatMap(item -> item.message().stream())
    .flatMap(message -> message.content().stream())
    .flatMap(content -> content.outputText().stream())
    .forEach(text -> System.out.println(text.text()));
```

By using alternating user and assistant messages, you capture the previous state of a conversation in one request to the model.

To manually share context across generated responses, include the model’s previous response output as input, and append that input to your next request.

For stateless reasoning-model requests, preserve every item in the response’s output array. The Responses API returns encrypted reasoning items by default. Replaying the complete output keeps reasoning items and assistant phase values intact. Models that support persisted reasoning can use reasoning.context: "all_turns" to render the available reasoning from earlier turns into the next sample. See preserve reasoning across calls.

In the following example, we ask the model to tell a joke, followed by a request for another joke. Appending previous responses to new requests in this way helps ensure conversations feel natural and retain the context of previous interactions.

```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputItem;
import java.util.ArrayList;

var history = new ArrayList<ResponseInputItem>();
history.add(
    ResponseInputItem.ofEasyInputMessage(
        EasyInputMessage.builder()
            .role(EasyInputMessage.Role.USER)
            .content("Tell me a joke.")
            .build()));

var first =
    client
        .responses()
        .create(
            ResponseCreateParams.builder()
                .model("gpt-6-astra")
                .inputOfResponse(history)
                .store(false)
                .build());
first.output().stream()
    .flatMap(item -> item.message().stream())
    .flatMap(message -> message.content().stream())
    .flatMap(content -> content.outputText().stream())
    .forEach(text -> System.out.println(text.text()));
first.output().stream()
    .map(item -> JsonValue.from(item).convert(ResponseInputItem.class))
    .forEach(history::add);
history.add(
    ResponseInputItem.ofEasyInputMessage(
        EasyInputMessage.builder()
            .role(EasyInputMessage.Role.USER)
            .content("Tell me another.")
            .build()));

client
    .responses()
    .create(
        ResponseCreateParams.builder()
            .model("gpt-6-astra")
            .inputOfResponse(history)
            .store(false)
            .build())
    .output()
    .stream()
    .flatMap(item -> item.message().stream())
    .flatMap(message -> message.content().stream())
    .flatMap(content -> content.outputText().stream())
    .forEach(text -> System.out.println(text.text()));
```

### OpenAI APIs for conversation state

Our APIs make it easier to manage conversation state automatically, so you don’t have to pass inputs manually with each turn of a conversation.

Using the Conversations API

The Conversations API works with the Responses API to persist conversation state as a long-running object with its own durable identifier. After creating a conversation object, you can keep using it across sessions, devices, or jobs.

Conversations store items, which can be messages, tool calls, tool outputs, and other data.

```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

var conversation = client.conversations().create();

System.out.println(conversation.id());
```
In a multi-turn interaction, you can pass the conversation into subsequent responses to persist state and share context across subsequent responses, rather than having to chain multiple response items together.

```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;

var conversation = client.conversations().create();

var response =
    client
        .responses()
        .create(
            ResponseCreateParams.builder()
                .model("gpt-6-astra")
                .conversation(conversation.id())
                .input("What are the five Ds of dodgeball?")
                .build());

response.output().stream()
    .flatMap(item -> item.message().stream())
    .flatMap(message -> message.content().stream())
    .flatMap(content -> content.outputText().stream())
    .forEach(text -> System.out.println(text.text()));
```
Passing context from the previous response

Another way to manage conversation state is to share context across generated responses with the previous_response_id parameter. This parameter lets you chain responses and create a threaded conversation.

```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;

var first =
    client
        .responses()
        .create(
            ResponseCreateParams.builder().model("gpt-6-astra").input("Tell me a joke.").build());

first.output().stream()
    .flatMap(item -> item.message().stream())
    .flatMap(message -> message.content().stream())
    .flatMap(content -> content.outputText().stream())
    .forEach(text -> System.out.println(text.text()));

var second =
    client
        .responses()
        .create(
            ResponseCreateParams.builder()
                .model("gpt-6-astra")
                .input("Explain why this is funny.")
                .previousResponseId(first.id())
                .build());
second.output().stream()
    .flatMap(item -> item.message().stream())
    .flatMap(message -> message.content().stream())
    .flatMap(content -> content.outputText().stream())
    .forEach(text -> System.out.println(text.text()));
```
In the following example, we ask the model to tell a joke. Separately, we ask the model to explain why it’s funny, and the model has all necessary context to deliver a good response.

```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;

var first =
    client
        .responses()
        .create(
            ResponseCreateParams.builder().model("gpt-6-astra").input("Tell me a joke.").build());

first.output().stream()
    .flatMap(item -> item.message().stream())
    .flatMap(message -> message.content().stream())
    .flatMap(content -> content.outputText().stream())
    .forEach(text -> System.out.println(text.text()));

var second =
    client
        .responses()
        .create(
            ResponseCreateParams.builder()
                .model("gpt-6-astra")
                .input("Explain why this is funny.")
                .previousResponseId(first.id())
                .build());
second.output().stream()
    .flatMap(item -> item.message().stream())
    .flatMap(message -> message.content().stream())
    .flatMap(content -> content.outputText().stream())
    .forEach(text -> System.out.println(text.text()));
```

## Background mode

Run long running tasks asynchronously in the background.

Agents like Codex and Deep Research show that reasoning models can take several minutes to solve complex problems. Background mode enables you to execute long-running tasks on models like GPT-5.2 and GPT-5.2 Pro reliably, without having to worry about timeouts or other connectivity issues.

Background mode kicks off these tasks asynchronously, and developers can poll response objects to check status over time. To start response generation in the background, make an API request with background set to true:

```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;

ResponseCreateParams params =
    ResponseCreateParams.builder()
        .model("gpt-6-astra")
        .input("Write a detailed market analysis.")
        .background(true)
        .build();

var response = client.responses().create(params);
System.out.println(response.status().orElseThrow());
```

### Polling background responses

To check the status of background requests, use the GET endpoint for Responses. Keep polling while the request is in the queued or in_progress state. When it leaves these states, it has reached a final (terminal) state.

```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseStatus;

ResponseCreateParams params =
    ResponseCreateParams.builder()
        .model("gpt-6-astra")
        .input("Write a very long novel about otters in space.")
        .background(true)
        .build();

var response = client.responses().create(params);
while (response.status().filter(ResponseStatus.QUEUED::equals).isPresent()
    || response.status().filter(ResponseStatus.IN_PROGRESS::equals).isPresent()) {
  System.out.println("Current status: " + response.status().orElseThrow());
  Thread.sleep(1000);
  response = client.responses().retrieve(response.id());
}
System.out.println("Final status: " + response.status().orElseThrow());
response.output().stream()
    .flatMap(item -> item.message().stream())
    .flatMap(message -> message.content().stream())
    .flatMap(content -> content.outputText().stream())
    .forEach(text -> System.out.println(text.text()));
```

### Cancelling a background response

```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

String responseId = "resp_123";

var response = client.responses().cancel(responseId);

System.out.println(response.status());
```

## Real-time streaming
```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseStreamEvent;

ResponseCreateParams params =
    ResponseCreateParams.builder()
        .model("gpt-6-astra")
        .input("Say 'double bubble bath' ten times fast.")
        .build();

try (StreamResponse<ResponseStreamEvent> stream = client.responses().createStreaming(params)) {
  stream.stream().forEach(System.out::println);
}
```
## Streaming a background response

You can create a background Response and start streaming events from it right away. This may be helpful if you expect the client to drop the stream and want the option of picking it back up later. To do this, create a Response with both background and stream set to true. You will want to keep track of a “cursor” corresponding to the sequence_number you receive in each streaming event.

```java
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseRetrieveParams;
import com.openai.models.responses.ResponseStreamEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

ResponseCreateParams params =
    ResponseCreateParams.builder()
        .model("gpt-6-astra")
        .input("Write a very long novel about otters in space.")
        .background(true)
        .build();

AtomicLong lastSequenceNumber = new AtomicLong(-1);
AtomicReference<String> responseId = new AtomicReference<>("");
AtomicBoolean streamCompleted = new AtomicBoolean(false);
JsonMapper json = new JsonMapper();
try (StreamResponse<ResponseStreamEvent> stream = client.responses().createStreaming(params)) {
  stream.stream()
      .forEach(
          event -> {
            lastSequenceNumber.set(json.valueToTree(event).path("sequence_number").asLong());
            event
                .created()
                .ifPresent(
                    created -> {
                      responseId.set(created.response().id());
                      System.out.println("response.created");
                    });
            event
                .outputTextDelta()
                .ifPresent(
                    delta -> {
                      System.out.println("response.output_text.delta");
                    });
            event
                .completed()
                .ifPresent(
                    completed -> {
                      streamCompleted.set(true);
                      System.out.println("response.completed");
                    });
          });
}
System.out.println(
    "Response " + responseId.get() + "; last sequence number " + lastSequenceNumber.get());
if (!streamCompleted.get()) {
  try (StreamResponse<ResponseStreamEvent> resumed =
      client
          .responses()
          .retrieveStreaming(
              ResponseRetrieveParams.builder()
                  .responseId(responseId.get())
                  .startingAfter(lastSequenceNumber.get())
                  .build())) {
    resumed.stream()
        .forEach(
            event ->
                event.outputTextDelta().ifPresent(delta -> System.out.println(delta.delta())));
  }
}
```

## Built-in tools (to test)

### Web search 
```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.WebSearchTool;

ResponseCreateParams params =
    ResponseCreateParams.builder()
        .model("gpt-6-astra")
        .input("What was a positive news story from today?")
        .addTool(WebSearchTool.builder().type(WebSearchTool.Type.WEB_SEARCH).build())
        .build();

client.responses().create(params).output().stream()
    .flatMap(item -> item.message().stream())
    .flatMap(message -> message.content().stream())
    .flatMap(content -> content.outputText().stream())
    .forEach(text -> System.out.println(text.text()));
```

### File search
```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;
import java.util.List;

String vectorStoreId = "<vector_store_id>";

ResponseCreateParams params =
    ResponseCreateParams.builder()
        .model("gpt-6-astra")
        .input("What is deep research by OpenAI?")
        .addFileSearchTool(List.of(vectorStoreId))
        .build();

client.responses().create(params).output().stream()
    .flatMap(item -> item.message().stream())
    .flatMap(message -> message.content().stream())
    .flatMap(content -> content.outputText().stream())
    .forEach(text -> System.out.println(text.text()));
```

### Code interpreter
```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.Tool;

ResponseCreateParams params =
    ResponseCreateParams.builder()
        .model("gpt-6-astra")
        .input("I need to solve the equation 3x + 11 = 14. Can you help me?")
        .instructions(
            "You are a personal math tutor. When asked a math question, write and run code to answer the question.")
        .addCodeInterpreterTool(
            Tool.CodeInterpreter.Container.CodeInterpreterToolAuto.builder().build())
        .build();

client.responses().create(params).output().stream()
    .flatMap(item -> item.message().stream())
    .flatMap(message -> message.content().stream())
    .flatMap(content -> content.outputText().stream())
    .forEach(text -> System.out.println(text.text()));
```

### Function calling
```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.responses.FunctionTool;
import com.openai.models.responses.ResponseCreateParams;
import java.util.List;
import java.util.Map;

ResponseCreateParams params =
    ResponseCreateParams.builder()
        .model("gpt-6-astra")
        .input("What is the weather like in Paris today?")
        .addTool(
            FunctionTool.builder()
                .name("get_weather")
                .description("Get current temperature for a given location.")
                .parameters(
                    FunctionTool.Parameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty(
                            "properties",
                            JsonValue.from(
                                Map.of(
                                    "location",
                                    Map.of(
                                        "type", "string",
                                        "description",
                                            "City and country e.g. Bogotá, Colombia"))))
                        .putAdditionalProperty("required", JsonValue.from(List.of("location")))
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build())
                .strict(true)
                .build())
        .build();

client.responses().create(params).output().forEach(System.out::println);
```

### Remote MCP
```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.Tool;

ResponseCreateParams params =
    ResponseCreateParams.builder()
        .model("gpt-6-astra")
        .input("Roll 2d4+1")
        .addTool(
            Tool.Mcp.builder()
                .serverLabel("dmcp")
                .serverDescription(
                    "A Dungeons and Dragons MCP server to assist with dice rolling.")
                .serverUrl("https://dmcp-server.deno.dev/mcp")
                .requireApproval(Tool.Mcp.RequireApproval.McpToolApprovalSetting.NEVER)
                .build())
        .build();

client.responses().create(params).output().stream()
    .flatMap(item -> item.message().stream())
    .flatMap(message -> message.content().stream())
    .flatMap(content -> content.outputText().stream())
    .forEach(text -> System.out.println(text.text()));
```
## Counting tokens

Get accurate input token counts before sending requests.

Token counting lets you determine how many input tokens a request will use before you send it to the model. Use it to:

Optimize prompts to fit within context limits
Estimate costs before making API calls
Route requests based on size (for example, smaller prompts to faster models)
Avoid surprises with images and files—no more character-based estimation
The input token count endpoint accepts the same input format as the Responses API. Pass text, messages, images, files, tools, or conversations—the API returns the exact count the model will receive.

The count includes formatting tokens used to represent request structure, such as message roles and boundaries. These tokens might not appear in the text or fields you tokenize locally.

Why use the token counting API?

Local tokenizers like tiktoken work for plain text, but they have limitations:

Images and files are not supported—estimates like characters / 4 are inaccurate
Tools and schemas add tokens that are hard to count locally
Model-specific behavior can change tokenization (for example, reasoning, caching)
The token counting API handles all of these. Use the same payload you would send to responses.create and get an accurate count. Then plug the result into your message validation or cost estimation flow.

### Count tokens in basic messages

```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.inputtokens.InputTokenCountParams;

var count =
    client
        .responses()
        .inputTokens()
        .count(
            InputTokenCountParams.builder()
                .model("gpt-6-astra")
                .input("Tell me a joke.")
                .build());

System.out.println(count.inputTokens());
```

### Count tokens in conversations (multi turn)
```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.inputtokens.InputTokenCountParams;
import java.util.List;

var count =
    client
        .responses()
        .inputTokens()
        .count(
            InputTokenCountParams.builder()
                .model("gpt-6-astra")
                .inputOfResponseInputItems(
                    List.of(
                        ResponseInputItem.ofEasyInputMessage(
                            EasyInputMessage.builder()
                                .role(EasyInputMessage.Role.USER)
                                .content("What is 2 + 2?")
                                .build()),
                        ResponseInputItem.ofEasyInputMessage(
                            EasyInputMessage.builder()
                                .role(EasyInputMessage.Role.ASSISTANT)
                                .content("2 + 2 equals 4.")
                                .build()),
                        ResponseInputItem.ofEasyInputMessage(
                            EasyInputMessage.builder()
                                .role(EasyInputMessage.Role.USER)
                                .content("What about 3 + 3?")
                                .build())))
                .build());

System.out.println(count.inputTokens());
```

### Count tokens with instructions

```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.inputtokens.InputTokenCountParams;

var count =
    client
        .responses()
        .inputTokens()
        .count(
            InputTokenCountParams.builder()
                .model("gpt-6-astra")
                .input("Explain quantum computing in one sentence.")
                .instructions("You are a helpful assistant that explains concepts simply.")
                .build());

System.out.println(count.inputTokens());
```

### Count tokens with tools

```java
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.responses.FunctionTool;
import com.openai.models.responses.inputtokens.InputTokenCountParams;
import java.util.List;
import java.util.Map;

var count =
    client
        .responses()
        .inputTokens()
        .count(
            InputTokenCountParams.builder()
                .model("gpt-6-astra")
                .input("What is the weather in San Francisco?")
                .addTool(
                    FunctionTool.builder()
                        .name("get_weather")
                        .description("Get the current weather in a location")
                        .strict(true)
                        .parameters(
                            FunctionTool.Parameters.builder()
                                .putAdditionalProperty("type", JsonValue.from("object"))
                                .putAdditionalProperty(
                                    "properties",
                                    JsonValue.from(
                                        Map.of("location", Map.of("type", "string"))))
                                .putAdditionalProperty(
                                    "required", JsonValue.from(List.of("location")))
                                .putAdditionalProperty(
                                    "additionalProperties", JsonValue.from(false))
                                .build())
                        .build())
                .build());

System.out.println(count.inputTokens());
```