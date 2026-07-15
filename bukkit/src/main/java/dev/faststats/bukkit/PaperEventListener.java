package dev.faststats.bukkit;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerPluginException;
import dev.faststats.SimpleContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

final class PaperEventListener implements Listener {
    private final Plugin plugin;
    private final SimpleContext context;

    PaperEventListener(final Plugin plugin, final SimpleContext context) {
        this.plugin = plugin;
        this.context = context;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerException(final ServerExceptionEvent event) {
        if (!(event.getException() instanceof ServerPluginException)) return;
        final ServerPluginException exception = (ServerPluginException) event.getException();
        if (!exception.getResponsiblePlugin().equals(plugin)) return;
        final Throwable report = exception.getCause() != null ? exception.getCause() : exception;
        context.errorTrackerService().ifPresent(service -> {
            service.globalErrorTracker().trackError(report).handled(false);
        });
    }
}
