import React, { useState, useEffect } from 'react';
import { View, StyleSheet, ScrollView, Alert } from 'react-native';
import { Modal, Portal, Text, TextInput, Button, Chip, HelperText } from 'react-native-paper';
import { itemApi } from '../api/items';
import { ItemCategory, ItemVariant } from '../types';

interface Props {
  visible: boolean;
  studentId: string;
  onDismiss: () => void;
  onPurchaseComplete: () => void;
}

export const PurchaseItemModal: React.FC<Props> = ({
  visible,
  studentId,
  onDismiss,
  onPurchaseComplete,
}) => {
  const [categories, setCategories] = useState<ItemCategory[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null);
  const [variants, setVariants] = useState<ItemVariant[]>([]);
  const [selectedVariantId, setSelectedVariantId] = useState<string | null>(null);
  const [quantity, setQuantity] = useState('1');

  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (visible) {
      loadCategories();
    }
  }, [visible]);

  const loadCategories = async () => {
    try {
      setIsLoading(true);
      const cats = await itemApi.listCategories();
      setCategories(cats);
      if (cats.length > 0) {
        setSelectedCategoryId(cats[0].id);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load categories');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (selectedCategoryId) {
      loadVariants(selectedCategoryId);
    }
  }, [selectedCategoryId]);

  const loadVariants = async (catId: string) => {
    try {
      const vars = await itemApi.listVariants({ itemCategoryId: catId });
      setVariants(vars);
      setSelectedVariantId(vars.length > 0 ? vars[0].id : null);
    } catch {
      // ignore
    }
  };

  const handlePurchase = async () => {
    if (!selectedVariantId) {
      setError('Please select an item');
      return;
    }
    const qty = parseInt(quantity, 10);
    if (isNaN(qty) || qty <= 0) {
      setError('Please enter a valid quantity');
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);
      const res = await itemApi.purchaseItem(studentId, {
        itemVariantId: selectedVariantId,
        quantity: qty,
      });

      if (res.warning) {
        Alert.alert('Purchase Recorded (Warning)', res.warning, [
          { text: 'OK', onPress: () => { onPurchaseComplete(); onDismiss(); } },
        ]);
      } else {
        Alert.alert('Success', 'Item purchase recorded and billed to student account');
        onPurchaseComplete();
        onDismiss();
      }
    } catch (err: any) {
      setError(err.message || 'Failed to record purchase');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Portal>
      <Modal visible={visible} onDismiss={onDismiss} contentContainerStyle={styles.modalContent}>
        <ScrollView>
          <Text variant="titleLarge" style={styles.title}>
            Sell Book / Uniform
          </Text>

          {error && <HelperText type="error" visible={true}>{error}</HelperText>}

          <Text variant="labelMedium" style={styles.sectionHeader}>
            Select Category
          </Text>
          <View style={styles.chipsRow}>
            {categories.map((c) => (
              <Chip
                key={c.id}
                selected={c.id === selectedCategoryId}
                onPress={() => setSelectedCategoryId(c.id)}
                style={styles.chip}
              >
                {c.name}
              </Chip>
            ))}
          </View>

          <Text variant="labelMedium" style={styles.sectionHeader}>
            Select Item Variant
          </Text>
          <View style={styles.variantsList}>
            {variants.map((v) => (
              <Button
                key={v.id}
                mode={v.id === selectedVariantId ? 'contained' : 'outlined'}
                onPress={() => setSelectedVariantId(v.id)}
                style={styles.variantBtn}
              >
                {`${v.label} ${v.size ? `(Size ${v.size})` : ''} - ₹${v.price} [Stock: ${v.stockQuantity}]`}
              </Button>
            ))}
            {variants.length === 0 && (
              <Text style={styles.emptyNotice}>No variants in this category</Text>
            )}
          </View>

          <TextInput
            label="Quantity"
            value={quantity}
            onChangeText={setQuantity}
            keyboardType="number-pad"
            mode="outlined"
            style={styles.input}
          />

          <View style={styles.actions}>
            <Button mode="text" onPress={onDismiss}>
              Cancel
            </Button>
            <Button
              mode="contained"
              onPress={handlePurchase}
              loading={isSubmitting}
              disabled={isSubmitting || !selectedVariantId}
              style={{ backgroundColor: '#607D8B' }}
            >
              Sell & Bill
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
  title: {
    fontWeight: '700',
    color: '#1A237E',
    marginBottom: 16,
  },
  sectionHeader: {
    color: '#666',
    marginTop: 8,
    marginBottom: 6,
  },
  chipsRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 12,
  },
  chip: {
    marginRight: 4,
  },
  variantsList: {
    gap: 6,
    marginBottom: 16,
  },
  variantBtn: {
    borderRadius: 8,
  },
  emptyNotice: {
    color: '#888',
    fontStyle: 'italic',
    paddingVertical: 8,
  },
  input: {
    marginBottom: 16,
    backgroundColor: '#fff',
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 8,
  },
});

export default PurchaseItemModal;
