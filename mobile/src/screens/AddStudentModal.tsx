import React, { useState, useEffect } from 'react';
import { View, StyleSheet, ScrollView, Alert } from 'react-native';
import { Modal, Portal, Text, TextInput, Button, SegmentedButtons, HelperText, Menu } from 'react-native-paper';
import { studentApi } from '../api/students';
import { useAcademicStore } from '../stores/academicStore';

interface Props {
  visible: boolean;
  onDismiss: () => void;
  onStudentAdded: () => void;
}

export const AddStudentModal: React.FC<Props> = ({ visible, onDismiss, onStudentAdded }) => {
  const { classes, sections, fetchClasses, fetchSections } = useAcademicStore();

  const [admissionNo, setAdmissionNo] = useState('');
  const [rollNo, setRollNo] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [gender, setGender] = useState('MALE');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [guardianName, setGuardianName] = useState('');
  const [guardianPhone, setGuardianPhone] = useState('');
  const [guardianEmail, setGuardianEmail] = useState('');
  const [address, setAddress] = useState('');

  const [selectedClassId, setSelectedClassId] = useState('');
  const [selectedSectionId, setSelectedSectionId] = useState('');
  const [classMenuVisible, setClassMenuVisible] = useState(false);
  const [sectionMenuVisible, setSectionMenuVisible] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (visible) {
      fetchClasses();
    }
  }, [visible]);

  useEffect(() => {
    if (selectedClassId) {
      fetchSections(selectedClassId);
    }
  }, [selectedClassId]);

  const handleSubmit = async () => {
    if (!admissionNo.trim() || !firstName.trim() || !lastName.trim() || !selectedSectionId) {
      setError('Please fill all required fields (*)');
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);
      await studentApi.create({
        admissionNo: admissionNo.trim(),
        rollNo: rollNo.trim() || undefined,
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        gender,
        dateOfBirth: dateOfBirth.trim() || undefined,
        guardianName: guardianName.trim() || undefined,
        guardianPhone: guardianPhone.trim() || undefined,
        guardianEmail: guardianEmail.trim() || undefined,
        address: address.trim() || undefined,
        sectionId: selectedSectionId,
      });

      Alert.alert('Success', 'Student enrolled successfully');
      onStudentAdded();
      onDismiss();
      // Reset fields
      setAdmissionNo('');
      setRollNo('');
      setFirstName('');
      setLastName('');
      setGuardianName('');
      setGuardianPhone('');
      setGuardianEmail('');
      setAddress('');
      setSelectedClassId('');
      setSelectedSectionId('');
    } catch (err: any) {
      setError(err.message || 'Failed to create student');
    } finally {
      setIsSubmitting(false);
    }
  };

  const selectedClassName = classes.find((c) => c.id === selectedClassId)?.name || 'Select Class *';
  const selectedSectionName = sections.find((s) => s.id === selectedSectionId)?.name || 'Select Section *';

  return (
    <Portal>
      <Modal visible={visible} onDismiss={onDismiss} contentContainerStyle={styles.modalContent}>
        <ScrollView style={styles.scroll}>
          <Text variant="titleLarge" style={styles.title}>
            New Student Admission
          </Text>

          {error && <HelperText type="error" visible={true}>{error}</HelperText>}

          <View style={styles.row}>
            <TextInput
              label="Admission No *"
              value={admissionNo}
              onChangeText={setAdmissionNo}
              mode="outlined"
              style={[styles.input, styles.half]}
            />
            <TextInput
              label="Roll No"
              value={rollNo}
              onChangeText={setRollNo}
              mode="outlined"
              style={[styles.input, styles.half]}
            />
          </View>

          <View style={styles.row}>
            <TextInput
              label="First Name *"
              value={firstName}
              onChangeText={setFirstName}
              mode="outlined"
              style={[styles.input, styles.half]}
            />
            <TextInput
              label="Last Name *"
              value={lastName}
              onChangeText={setLastName}
              mode="outlined"
              style={[styles.input, styles.half]}
            />
          </View>

          <Text variant="labelMedium" style={styles.label}>
            Gender
          </Text>
          <SegmentedButtons
            value={gender}
            onValueChange={setGender}
            buttons={[
              { value: 'MALE', label: 'Male' },
              { value: 'FEMALE', label: 'Female' },
              { value: 'OTHER', label: 'Other' },
            ]}
            style={styles.segmented}
          />

          {/* Class & Section pickers */}
          <View style={styles.row}>
            <Menu
              visible={classMenuVisible}
              onDismiss={() => setClassMenuVisible(false)}
              anchor={
                <Button
                  mode="outlined"
                  onPress={() => setClassMenuVisible(true)}
                  style={[styles.pickerButton, styles.half]}
                >
                  {selectedClassName}
                </Button>
              }
            >
              {classes.map((cls) => (
                <Menu.Item
                  key={cls.id}
                  title={cls.name}
                  onPress={() => {
                    setSelectedClassId(cls.id);
                    setSelectedSectionId('');
                    setClassMenuVisible(false);
                  }}
                />
              ))}
            </Menu>

            <Menu
              visible={sectionMenuVisible}
              onDismiss={() => setSectionMenuVisible(false)}
              anchor={
                <Button
                  mode="outlined"
                  onPress={() => setSectionMenuVisible(true)}
                  disabled={!selectedClassId}
                  style={[styles.pickerButton, styles.half]}
                >
                  {selectedSectionName}
                </Button>
              }
            >
              {sections.map((sec) => (
                <Menu.Item
                  key={sec.id}
                  title={sec.name}
                  onPress={() => {
                    setSelectedSectionId(sec.id);
                    setSectionMenuVisible(false);
                  }}
                />
              ))}
            </Menu>
          </View>

          <TextInput
            label="Guardian Name"
            value={guardianName}
            onChangeText={setGuardianName}
            mode="outlined"
            style={styles.input}
          />

          <TextInput
            label="Guardian Phone"
            value={guardianPhone}
            onChangeText={setGuardianPhone}
            keyboardType="phone-pad"
            mode="outlined"
            style={styles.input}
          />

          <TextInput
            label="Guardian Email"
            value={guardianEmail}
            onChangeText={setGuardianEmail}
            keyboardType="email-address"
            autoCapitalize="none"
            mode="outlined"
            style={styles.input}
          />

          <TextInput
            label="Address"
            value={address}
            onChangeText={setAddress}
            multiline
            numberOfLines={2}
            mode="outlined"
            style={styles.input}
          />

          <View style={styles.actions}>
            <Button mode="text" onPress={onDismiss} style={styles.actionBtn}>
              Cancel
            </Button>
            <Button
              mode="contained"
              onPress={handleSubmit}
              loading={isSubmitting}
              disabled={isSubmitting}
              style={[styles.actionBtn, { backgroundColor: '#3F51B5' }]}
            >
              Enroll Student
            </Button>
          </View>
        </ScrollView>
      </Modal>
    </Portal>
  );
};

const styles = StyleSheet.create({
  modalContent: {
    backgroundColor: '#fff',
    margin: 20,
    borderRadius: 12,
    maxHeight: '85%',
    padding: 20,
  },
  scroll: {
    paddingBottom: 16,
  },
  title: {
    fontWeight: '700',
    color: '#1A237E',
    marginBottom: 16,
  },
  label: {
    color: '#666',
    marginTop: 8,
    marginBottom: 4,
  },
  row: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 8,
  },
  half: {
    flex: 1,
  },
  input: {
    marginBottom: 10,
    backgroundColor: '#fff',
  },
  segmented: {
    marginBottom: 12,
  },
  pickerButton: {
    marginVertical: 4,
    justifyContent: 'center',
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 8,
    marginTop: 16,
  },
  actionBtn: {
    borderRadius: 6,
  },
});

export default AddStudentModal;
