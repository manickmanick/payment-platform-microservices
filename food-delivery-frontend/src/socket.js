
import api from "./api/axios";

/**
 * Poll driver location only while the tracking component is mounted.
 *
 * - First request executes immediately.
 * - Subsequent requests execute every 5 seconds.
 * - Prevents overlapping HTTP requests.
 * - Stops polling when the component unmounts.
 */
export function subscribeToDriverLocation(orderId, onLocation) {
  let active = true;
  let requestInProgress = false;

  const refresh = async () => {
    if (!active || requestInProgress) {
      return;
    }

    requestInProgress = true;

    try {
      const { data } = await api.get(
        `/orders/${orderId}/driver-location`
      );

      if (!active) {
        return;
      }

      // Ignore invalid location responses.
      if (
        typeof data?.latitude !== "number" ||
        typeof data?.longitude !== "number"
      ) {
        return;
      }

      onLocation(data);
    } catch (error) {
      // Location may not be available yet.
      // The next polling cycle will try again.
      if (active) {
        console.warn(
          `Unable to fetch driver location for order ${orderId}`,
          error
        );
      }
    } finally {
      requestInProgress = false;
    }
  };

  // Fetch the first location immediately.
  refresh();

  // Fetch updated location every 5 seconds.
  const timer = window.setInterval(refresh, 5000);

  // Cleanup function.
  return () => {
    active = false;
    window.clearInterval(timer);
  };
}

/**
 * Poll notifications.
 * Kept unchanged in functionality for other screens.
 */
export function subscribeToNotifications(_userId, onNotification) {
  let active = true;
  const seen = new Set();
  let initialized = false;

  const refresh = async () => {
    try {
      const { data } = await api.get("/notifications");

      if (!active) {
        return;
      }

      const rows = data.content || [];

      for (const row of rows) {
        if (seen.has(row.id)) {
          continue;
        }

        seen.add(row.id);

        if (initialized) {
          onNotification(row);
        }
      }

      initialized = true;
    } catch {
      // Retry during the next polling cycle.
    }
  };

  refresh();

  const timer = window.setInterval(refresh, 15000);

  return () => {
    active = false;
    window.clearInterval(timer);
  };
}


export function subscribeToOrder(orderId, onUpdate) {
  let active = true;
  let requestInProgress = false;
  let lastSignature = "";

  const refresh = async () => {
    if (!active || requestInProgress) {
      return;
    }

    requestInProgress = true;

    try {
      const { data } = await api.get(`/orders/${orderId}`);

      if (!active) {
        return;
      }

      const signature = `${data.status}:${data.updatedAt || ""}`;

      if (signature !== lastSignature) {
        lastSignature = signature;
        onUpdate(data);
      }
    } catch (error) {
      if (active) {
        console.warn(`Unable to refresh order ${orderId}`, error);
      }
    } finally {
      requestInProgress = false;
    }
  };

  refresh();

  const timer = window.setInterval(refresh, 10000);

  return () => {
    active = false;
    window.clearInterval(timer);
  };
}

