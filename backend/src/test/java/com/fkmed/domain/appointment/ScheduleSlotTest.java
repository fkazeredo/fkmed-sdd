package com.fkmed.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SPEC-0009 BR6: the slot's in-memory capacity guard (the version lock is exercised by the IT). */
class ScheduleSlotTest {

  @Test
  void occupy_reducesRemaining_untilFull_thenThrows() {
    ScheduleSlot slot = slotWithCapacity(2);
    assertThat(slot.remaining()).isEqualTo(2);

    slot.occupy();
    assertThat(slot.remaining()).isEqualTo(1);
    assertThat(slot.hasCapacity()).isTrue();

    slot.occupy();
    assertThat(slot.remaining()).isZero();
    assertThat(slot.hasCapacity()).isFalse();

    assertThatThrownBy(slot::occupy).isInstanceOf(SlotUnavailableException.class);
  }

  @Test
  void release_freesASeat_neverBelowZero() {
    ScheduleSlot slot = slotWithCapacity(1);
    slot.occupy();
    slot.release();
    assertThat(slot.remaining()).isEqualTo(1);

    slot.release();
    assertThat(slot.remaining()).isEqualTo(1);
  }

  private static ScheduleSlot slotWithCapacity(int capacity) {
    try {
      ScheduleSlot slot = new ScheduleSlot();
      set(slot, "id", UUID.randomUUID());
      set(slot, "agendaId", UUID.randomUUID());
      set(slot, "slotDate", LocalDate.now());
      set(slot, "slotTime", LocalTime.of(9, 0));
      set(slot, "capacity", capacity);
      set(slot, "occupied", 0);
      return slot;
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void set(Object target, String field, Object value)
      throws ReflectiveOperationException {
    Field f = target.getClass().getDeclaredField(field);
    f.setAccessible(true);
    f.set(target, value);
  }
}
