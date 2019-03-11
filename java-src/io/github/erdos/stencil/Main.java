package io.github.erdos.stencil;

import io.github.erdos.stencil.standalone.ArgsParser;
import io.github.erdos.stencil.standalone.StandaloneApplication;
import io.github.erdos.stencil.standalone.StencilArgsParser;

import java.io.IOException;

import static io.github.erdos.stencil.impl.ClojureHelper.callShutdownAgents;

public class Main {

    public static void main(String... args) throws IOException {
        try {
            final ArgsParser.ParseResult parsed = StencilArgsParser.parse(args);
            final StandaloneApplication app = new StandaloneApplication(parsed);

            app.run();
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR:");
            System.out.println(e.getMessage());
            System.exit(2);
        } finally {
            // stop Clojure thread pools
            callShutdownAgents();
        }
    }
}