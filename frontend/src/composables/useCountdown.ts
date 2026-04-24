import { computed, onUnmounted, ref, type ComputedRef, type Ref } from 'vue';

export interface Countdown {
  remaining: Ref<number>;
  isRunning: ComputedRef<boolean>;
  start: () => void;
  reset: () => void;
}

export function useCountdown(initialSeconds: number): Countdown {
  const remaining = ref(initialSeconds);
  const running = ref(false);
  let timer: ReturnType<typeof setInterval> | null = null;

  const stop = () => {
    if (timer !== null) {
      clearInterval(timer);
      timer = null;
    }
    running.value = false;
  };

  const reset = () => {
    stop();
    remaining.value = initialSeconds;
  };

  const start = () => {
    if (running.value) return;
    remaining.value = initialSeconds;
    running.value = true;
    timer = setInterval(() => {
      remaining.value -= 1;
      if (remaining.value <= 0) {
        reset();
      }
    }, 1000);
  };

  onUnmounted(stop);

  return {
    remaining,
    isRunning: computed(() => running.value),
    start,
    reset,
  };
}
