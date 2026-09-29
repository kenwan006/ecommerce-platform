import React, { useEffect, useState } from 'react';
import { api } from '../lib/api';

export default function Warehouse() {
  const [inventory, setInventory] = useState([]);
  const [movements, setMovements] = useState([]);
  const [fulfillments, setFulfillments] = useState([]);
  const [error, setError] = useState('');
  const [shippingId, setShippingId] = useState(null);
  const [tracking, setTracking] = useState({});
  const [adjustment, setAdjustment] = useState({ productId: '', quantity: '', type: 'IMPORT', note: '' });
  const [isAdjusting, setIsAdjusting] = useState(false);

  const load = async () => {
    try {
      await api.syncWarehouseProducts();
      const [levels, orders, recentMovements] = await Promise.all([
        api.getInventory(), api.getFulfillments(), api.getInventoryMovements(),
      ]);
      setInventory(levels);
      setFulfillments(orders);
      setMovements(recentMovements);
      setError('');
    } catch (loadError) {
      setError(loadError.message);
    }
  };

  const updateInventory = async event => {
    event.preventDefault();
    if (!adjustment.productId || !adjustment.quantity) {
      setError('Choose a product and enter a quantity.');
      return;
    }
    setIsAdjusting(true);
    try {
      await api.adjustInventory({
        ...adjustment,
        productId: Number(adjustment.productId),
        quantity: Number(adjustment.quantity),
      });
      setAdjustment({ productId: '', quantity: '', type: 'IMPORT', note: '' });
      await load();
    } catch (adjustError) {
      setError(adjustError.message);
    } finally {
      setIsAdjusting(false);
    }
  };

  useEffect(() => { load(); }, []);

  const ship = async fulfillment => {
    const trackingNumber = tracking[fulfillment.id]?.trim();
    if (!trackingNumber) {
      setError('Enter a tracking number before marking an order shipped.');
      return;
    }
    setShippingId(fulfillment.id);
    try {
      await api.shipFulfillment(fulfillment.id, {
        carrier: 'Demo Carrier',
        trackingNumber,
      });
      setTracking(values => ({ ...values, [fulfillment.id]: '' }));
      await load();
    } catch (shipError) {
      setError(shipError.message);
    } finally {
      setShippingId(null);
    }
  };

  return (
    <section className="warehouse-page">
      <div className="warehouse-heading">
        <p>WAREHOUSE OPERATIONS</p>
        <h1>Inventory &amp; shipping</h1>
        <span>Paid orders appear below when they are ready for picking.</span>
      </div>
      {error && <p className="warehouse-error">{error}</p>}

      <div className="warehouse-grid">
        <article className="warehouse-card">
          <h2>Available inventory</h2>
          <div className="warehouse-table">
            <div className="warehouse-row warehouse-labels"><span>Product</span><span>On hand</span><span>Reserved</span><span>Available</span></div>
            {inventory.map(level => (
              <div className="warehouse-row" key={level.productId}>
                <span>{level.productName}</span><span>{level.onHandQuantity}</span>
                <span>{level.reservedQuantity}</span><strong>{level.availableQuantity}</strong>
              </div>
            ))}
            {!inventory.length && <p className="warehouse-empty">No inventory records yet.</p>}
          </div>
        </article>

        <article className="warehouse-card">
          <h2>Import or export stock</h2>
          <p className="warehouse-help">Imports add physical units. Exports remove only units that are not reserved for customers.</p>
          <form className="inventory-adjustment" onSubmit={updateInventory}>
            <label>Product
              <select value={adjustment.productId} onChange={event => setAdjustment(values => ({ ...values, productId: event.target.value }))} required>
                <option value="">Choose a product</option>
                {inventory.map(level => <option value={level.productId} key={level.productId}>{level.productName}</option>)}
              </select>
            </label>
            <label>Movement
              <select value={adjustment.type} onChange={event => setAdjustment(values => ({ ...values, type: event.target.value }))}>
                <option value="IMPORT">Import / receive</option>
                <option value="EXPORT">Export / remove</option>
              </select>
            </label>
            <label>Quantity<input type="number" min="1" value={adjustment.quantity} onChange={event => setAdjustment(values => ({ ...values, quantity: event.target.value }))} required /></label>
            <label className="adjustment-note">Note (optional)<input maxLength="1000" value={adjustment.note} onChange={event => setAdjustment(values => ({ ...values, note: event.target.value }))} placeholder="Purchase order, damage, transfer…" /></label>
            <button className="primary" disabled={isAdjusting}>{isAdjusting ? 'Updating…' : 'Update inventory'}</button>
          </form>
        </article>

        <article className="warehouse-card">
          <h2>Orders ready to ship</h2>
          <div className="fulfillment-list">
            {fulfillments.map(fulfillment => (
              <div className="fulfillment" key={fulfillment.id}>
                <div>
                  <strong>Order #{fulfillment.orderId}</strong>
                  <span>{fulfillment.items.map(item => `${item.productName} × ${item.quantity}`).join(', ')}</span>
                </div>
                <div className="ship-action">
                  <input
                    aria-label={`Tracking number for order ${fulfillment.orderId}`}
                    placeholder="Tracking number"
                    value={tracking[fulfillment.id] || ''}
                    onChange={event => setTracking(values => ({ ...values, [fulfillment.id]: event.target.value }))}
                  />
                  <button className="primary" disabled={shippingId === fulfillment.id} onClick={() => ship(fulfillment)}>
                    {shippingId === fulfillment.id ? 'Shipping…' : 'Mark shipped'}
                  </button>
                </div>
              </div>
            ))}
            {!fulfillments.length && <p className="warehouse-empty">No paid orders are waiting to ship.</p>}
          </div>
        </article>

        <article className="warehouse-card warehouse-movements">
          <h2>Recent movement history</h2>
          {movements.map(movement => (
            <div className="movement" key={movement.id}>
              <div><strong>{movement.productName}</strong><span>{movement.note || '—'}</span></div>
              <div><b className={movement.type === 'IMPORT' || movement.type === 'INITIAL_LOAD' ? 'movement-in' : 'movement-out'}>
                {movement.type === 'IMPORT' || movement.type === 'INITIAL_LOAD' ? '+' : '-'}{movement.quantity}
              </b><span>{movement.type.replace('_', ' ')}</span></div>
            </div>
          ))}
          {!movements.length && <p className="warehouse-empty">No inventory movements yet.</p>}
        </article>
      </div>
    </section>
  );
}
