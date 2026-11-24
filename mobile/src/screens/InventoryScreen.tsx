import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, FlatList, RefreshControl, Alert } from 'react-native';
import { Text, Surface, Chip, Divider, Modal, Portal, TextInput, Button, HelperText } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { itemApi } from '../api/items';
import { InventoryItem, ItemCategory } from '../types';
import { useAuthStore } from '../stores/authStore';
import { formatCurrency } from '../utils/format';

export const InventoryScreen: React.FC = () => {
  const { role } = useAuthStore();
  const [categories, setCategories] = useState<ItemCategory[]>([]);
  const [selectedCatId, setSelectedCatId] = useState<string | null>(null);
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [lowStockBelow, setLowStockBelow] = useState<number | undefined>(undefined);
  const [isLoading, setIsLoading] = useState(false);

  // Restock / Adjust Modal
  const [selectedItem, setSelectedItem] = useState<InventoryItem | null>(null);
  const [stockQuantity, setStockQuantity] = useState('');
  const [stockNote, setStockNote] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);

  useEffect(() => {
    itemApi.listCategories().then(setCategories).catch(() => {});
  }, []);

  const loadInventory = useCallback(async () => {
    try {
      setIsLoading(true);
      const res = await itemApi.listInventory({
        itemCategoryId: selectedCatId || undefined,
        lowStockBelow: lowStockBelow,
      });
      setItems(res.items || []);
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
    }
  }, [selectedCatId, lowStockBelow]);

  useEffect(() => {
    loadInventory();
  }, [loadInventory]);

  const handleRestock = async () => {
    if (!selectedItem) return;
    const qty = parseInt(stockQuantity, 10);
    if (isNaN(qty) || qty <= 0) {
      setModalError('Please enter a valid quantity');
      return;
    }

    try {
      setIsSubmitting(true);
      setModalError(null);
      await itemApi.restock(selectedItem.id, qty, stockNote.trim() || undefined);
      Alert.alert('Success', `Restocked ${qty} units of ${selectedItem.label}`);
      setSelectedItem(null);
      setStockQuantity('');
      setStockNote('');
      loadInventory();
    } catch (err: any) {
      setModalError(err.message || 'Restock failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  const canManage = role === 'ADMIN' || role === 'ACCOUNTANT' || role === 'MASTER';

  return (
    <View style={styles.container}>
      {/* Category and Low Stock Filters */}
      <View style={styles.filterRow}>
        <Chip
          selected={lowStockBelow === 5}
          onPress={() => setLowStockBelow(lowStockBelow === 5 ? undefined : 5)}
          style={[styles.filterChip, lowStockBelow === 5 ? styles.lowStockChip : null]}
        >
          Low Stock (&lt; 5)
        </Chip>
        <Chip
          selected={selectedCatId === null}
          onPress={() => setSelectedCatId(null)}
          style={styles.filterChip}
        >
          All Items
        </Chip>
        {categories.map((c) => (
          <Chip
            key={c.id}
            selected={selectedCatId === c.id}
            onPress={() => setSelectedCatId(selectedCatId === c.id ? null : c.id)}
            style={styles.filterChip}
          >
            {c.name}
          </Chip>
        ))}
      </View>

      {/* Inventory Items List */}
      <FlatList
        data={items}
        keyExtractor={(item) => item.id}
        refreshControl={<RefreshControl refreshing={isLoading} onRefresh={loadInventory} />}
        contentContainerStyle={styles.listContent}
        ItemSeparatorComponent={() => <Divider style={styles.divider} />}
        ListEmptyComponent={
          !isLoading ? (
            <View style={styles.emptyContainer}>
              <MaterialCommunityIcons name="package-variant" size={48} color="#999" />
              <Text style={styles.emptyText}>No inventory items found</Text>
            </View>
          ) : undefined
        }
        renderItem={({ item }) => {
          const isLow = item.stockQuantity < 5;
          return (
            <Surface style={styles.itemCard} elevation={1}>
              <View style={styles.itemHeader}>
                <View style={{ flex: 1 }}>
                  <Text variant="titleMedium" style={styles.itemLabel}>
                    {item.label}
                  </Text>
                  <Text style={styles.catText}>
                    Category: {item.category}
                    {item.size ? ` | Size: ${item.size}` : ''}
                    {item.class ? ` | Class: ${item.class}` : ''}
                  </Text>
                  <Text style={styles.priceText}>Price: ₹{formatCurrency(item.price)}</Text>
                </View>

                <View style={styles.stockCol}>
                  <View style={[styles.stockBadge, isLow ? styles.lowBadge : styles.okBadge]}>
                    <Text style={[styles.stockText, isLow ? styles.lowStockText : styles.okStockText]}>
                      {item.stockQuantity} in stock
                    </Text>
                  </View>
                  {canManage && (
                    <Button
                      mode="contained-tonal"
                      compact
                      onPress={() => {
                        setSelectedItem(item);
                        setStockQuantity('');
                        setStockNote('');
                        setModalError(null);
                      }}
                      style={styles.restockBtn}
                    >
                      Restock
                    </Button>
                  )}
                </View>
              </View>
            </Surface>
          );
        }}
      />

      {/* Restock Modal */}
      <Portal>
        <Modal
          visible={!!selectedItem}
          onDismiss={() => setSelectedItem(null)}
          contentContainerStyle={styles.modalContent}
        >
          <Text variant="titleLarge" style={styles.modalTitle}>
            Restock Inventory
          </Text>
          <Text style={styles.modalSub}>{selectedItem?.label}</Text>

          {modalError && <HelperText type="error" visible={true}>{modalError}</HelperText>}

          <TextInput
            label="Quantity to Add *"
            value={stockQuantity}
            onChangeText={setStockQuantity}
            keyboardType="number-pad"
            mode="outlined"
            style={styles.input}
          />

          <TextInput
            label="Note (Vendor, batch, or invoice ref)"
            value={stockNote}
            onChangeText={setStockNote}
            mode="outlined"
            style={styles.input}
          />

          <View style={styles.modalActions}>
            <Button mode="text" onPress={() => setSelectedItem(null)}>
              Cancel
            </Button>
            <Button
              mode="contained"
              onPress={handleRestock}
              loading={isSubmitting}
              disabled={isSubmitting}
              style={{ backgroundColor: '#607D8B' }}
            >
              Add to Stock
            </Button>
          </View>
        </Modal>
      </Portal>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8F9FA',
  },
  filterRow: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 10,
    flexWrap: 'wrap',
    gap: 6,
  },
  filterChip: {
    marginRight: 4,
  },
  lowStockChip: {
    backgroundColor: '#FFEBEE',
  },
  listContent: {
    padding: 16,
    paddingTop: 4,
    paddingBottom: 40,
  },
  divider: {
    height: 8,
    backgroundColor: 'transparent',
  },
  itemCard: {
    padding: 14,
    backgroundColor: '#fff',
    borderRadius: 12,
  },
  itemHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  itemLabel: {
    fontWeight: '700',
    color: '#212121',
  },
  catText: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  priceText: {
    fontSize: 13,
    color: '#00796B',
    fontWeight: '600',
    marginTop: 4,
  },
  stockCol: {
    alignItems: 'flex-end',
    gap: 8,
  },
  stockBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  lowBadge: {
    backgroundColor: '#FFEBEE',
  },
  okBadge: {
    backgroundColor: '#E8F5E9',
  },
  stockText: {
    fontWeight: '700',
    fontSize: 12,
  },
  lowStockText: {
    color: '#D32F2F',
  },
  okStockText: {
    color: '#388E3C',
  },
  restockBtn: {
    borderRadius: 6,
  },
  emptyContainer: {
    alignItems: 'center',
    paddingTop: 60,
  },
  emptyText: {
    marginTop: 10,
    fontSize: 15,
    color: '#999',
  },
  modalContent: {
    backgroundColor: '#fff',
    margin: 20,
    borderRadius: 12,
    padding: 20,
  },
  modalTitle: {
    fontWeight: '700',
    color: '#37474F',
  },
  modalSub: {
    fontSize: 13,
    color: '#666',
    marginBottom: 14,
    marginTop: 2,
  },
  input: {
    marginBottom: 10,
    backgroundColor: '#fff',
  },
  modalActions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 8,
    marginTop: 12,
  },
});

export default InventoryScreen;
