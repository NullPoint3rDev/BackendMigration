package org.alloy.models;

/**
 * Единый справочник названий ошибок оборудования по коду (1–23).
 * Используется в отчёте по неисправностям и на странице мониторинга аппарата.
 */
public final class EquipmentErrorMessages {

    private EquipmentErrorMessages() {}

    /**
     * Описания ошибок по номеру (индекс = код ошибки − 1).
     * Соответствие битовому полю аппарата: Errors1 (коды 1–10), Errors2 (коды 17–21), 11–16 и 22–23 — резерв.
     */
    public static final String[] MESSAGES = {
            "Ошибка драйвера подающего механизма",                 // 1  WF_DriverAlarm
            "Реверс энкодера подающего механизма",                 // 2  WF_EncoderRotateAlarm
            "Нет сигнала от энк. подающего механизма",            // 3  WF_EncoderConnectionAlarm
            "Отказ связи с подающим механизмом",                  // 4  WF_ModbusConnectironAlarm
            "Ошибка ограничения драйвера платы сварки",           // 5  WB_DriverLimitAlarm
            "Отказ связи с платой сварки",                        // 6  WB_ModbusConnectionAlarm
            "Ошибка превышения макс. тока платы сварки",          // 7  WB_MaxCurrentAlarm
            "Ошибка калибровки датчиков платы сварки",            // 8  WB_SensorCalibrateAlarm
            "Ошибка обратной связи по напряжению платы сварки",   // 9  WB_VoltageFdbAlarm
            "Ошибка обратной связи по мощности платы сварки",       // 10 WB_PowerFdbAlarm
            "Ошибка 11", "Ошибка 12", "Ошибка 13", "Ошибка 14", "Ошибка 15", "Ошибка 16",  // резерв в протоколе
            "Перегрев БВО",                                       // 17 ChillerOverheat
            "Отказ связи с БВО",                                   // 18 ChillerConnectionError
            "Пустая помпа БВО (нет жидкости)",                     // 19 ChillerEmptyPump
            "Обрыв датчика температуры жидкости БВО",              // 20 ChillerSensorBreak
            "КЗ датчика температуры жидкости БВО",                 // 21 ChillerSensorSC
            "Ошибка 22", "Ошибка 23"                               // резерв
    };

    /**
     * Возвращает человекочитаемое название ошибки по коду.
     * @param errorCodeObj код ошибки (число 1–23 или строка с числом); может быть null/пусто
     * @return название из справочника, либо «(без кода)», либо переданная строка как есть
     */
    public static String resolve(Object errorCodeObj) {
        if (errorCodeObj == null) return "(без кода)";
        String s = String.valueOf(errorCodeObj).trim();
        if (s.isEmpty()) return "(без кода)";
        try {
            int num = Integer.parseInt(s);
            if (num >= 1 && num <= MESSAGES.length) {
                return MESSAGES[num - 1];
            }
        } catch (NumberFormatException ignored) { }
        return s;
    }
}
