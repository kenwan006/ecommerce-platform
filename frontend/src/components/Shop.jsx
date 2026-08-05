import React from 'react';
import ProductCard from './ProductCard';

export default function Shop({ products, onAddToCart }) {
  const scrollToCatalogue = () => document.querySelector('#catalogue')?.scrollIntoView({ behavior: 'smooth' });
  return (
    <>
    <section className="hero">
      <p>NEW SEASON · EVERYDAY OBJECTS</p>
        <h1>Useful things,<br />beautifully made.</h1>
      <button onClick={scrollToCatalogue}>Explore the collection</button>
    </section>
    <section id="catalogue">
        <div className="section-heading"><p>THE COLLECTION</p><h2>Made for the long haul.</h2></div>
        <div className="grid">
          {products.map(product => <ProductCard key={product.id} product={product} onAdd={onAddToCart} />)}
        </div>
    </section>
    </>
  );
}
