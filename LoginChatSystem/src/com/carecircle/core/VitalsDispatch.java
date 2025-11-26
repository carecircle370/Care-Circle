package com.carecircle.core;

import java.util.List;

public interface VitalsDispatch {
    List<VitalsRecord> listByPatient(String patientId);
    List<VitalsRecord> listAll();
}
