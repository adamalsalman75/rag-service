
package dev.alsalman.ragservice;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Route("")
@SpringComponent
@UIScope
public class RagStreamingView extends VerticalLayout {

    private final RagService ragService;
    private final TextArea responseArea;
    private final Button streamButton;

    @Autowired
    public RagStreamingView(RagService ragService) {
        this.ragService = ragService;

        setSpacing(true);
        setPadding(true);
        setDefaultHorizontalComponentAlignment(Alignment.STRETCH);

        add(new H1("RAG Streaming Demo"));

        responseArea = new TextArea("Streaming Response");
        responseArea.setWidthFull();
        responseArea.setHeight("400px");
        responseArea.setReadOnly(true);

        streamButton = new Button("Start RAG Stream", this::startStreaming);

        add(responseArea, streamButton);
    }

    private void startStreaming(ClickEvent<Button> event) {
        streamButton.setEnabled(false);
        responseArea.clear();
        responseArea.setValue("Starting stream...\n");

        UI ui = UI.getCurrent();

        try {
            Flux<String> ragStream = ragService.query("Explain RagService and RagController");

            ragStream
                    .subscribeOn(Schedulers.boundedElastic()) // Subscribe on background thread
                    .publishOn(Schedulers.boundedElastic())   // Process on background thread
                    .subscribe(
                            chunk -> {
                                // Update UI from background thread with proper access
                                ui.access(() -> {
                                    try {
                                        String currentText = responseArea.getValue();
                                        responseArea.setValue(currentText + chunk);
                                        // Force UI update
                                        ui.push();
                                    } catch (Exception e) {
                                        System.err.println("Error updating UI: " + e.getMessage());
                                    }
                                });
                            },
                            error -> {
                                System.err.println("Stream error: " + error.getMessage());
                                error.printStackTrace();
                                ui.access(() -> {
                                    responseArea.setValue(responseArea.getValue() + "\nError: " + error.getMessage());
                                    streamButton.setEnabled(true);
                                    ui.push();
                                });
                            },
                            () -> {
                                System.out.println("Stream completed successfully");
                                ui.access(() -> {
                                    responseArea.setValue(responseArea.getValue() + "\n\n--- Stream completed ---");
                                    streamButton.setEnabled(true);
                                    ui.push();
                                });
                            }
                    );
        } catch (Exception e) {
            System.err.println("Error starting stream: " + e.getMessage());
            e.printStackTrace();
            responseArea.setValue("Failed to start stream: " + e.getMessage());
            streamButton.setEnabled(true);
        }
    }
}