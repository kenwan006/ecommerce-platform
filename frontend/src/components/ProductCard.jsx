import React from 'react';
import { formatCurrency } from '../lib/formatters';

export default function ProductCard({ product, onAdd }) {
  return <article className="card" data-testid={`product-${product.id}`}>
    <img src={product.imageUrl} alt={product.name} />
    <div>
      <h3>{product.name}</h3>
      <p>{product.description}</p>
      <strong>{formatCurrency(product.price)}</strong>
      <button
        aria-label={`Add ${product.name} to bag`}
        disabled={!product.stock}
        onClick={() => onAdd(product)}
      >
        {product.stock ? 'Add to bag' : 'Sold out'}
      </button>
    </div>
  </article>;
}
