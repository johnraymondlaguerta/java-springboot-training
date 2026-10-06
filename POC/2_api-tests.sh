#!/usr/bin/env bash
# Every call goes through the API gateway (the single public port 8080).

GATEWAY='http://localhost:8080'

# Pick a working python for pretty printing (falls back to raw output)
PY=""
for c in python3 python py; do
  if "$c" -c "import json" >/dev/null 2>&1; then PY="$c"; break; fi
done

pretty() {
  if [ -n "$PY" ]; then "$PY" -m json.tool; else cat; fi
}

extract_id() {
  pretty | sed -n 's/.*"id": *"\([a-z0-9]*\)".*/\1/p' | head -1
}

post_product() {   # sku name category price stock specs-json
  curl -sX POST "$GATEWAY/api/product" \
    -H 'accept: */*' -H 'Content-Type: application/json' \
    -d '{
      "sku": "'"${1}"'",
      "name": "'"${2}"'",
      "category": "'"${3}"'",
      "price": '"${4}"',
      "stock": '"${5}"',
      "specs": '"${6}"'
  }'
}

post_customer() {  # name email
  curl -sX POST "$GATEWAY/api/customer" \
    -H 'accept: */*' -H 'Content-Type: application/json' \
    -d '{
      "name": "'"${1}"'",
      "email": "'"${2}"'"
  }'
}

post_order() {     # customerId productId quantity
  curl -sX POST "$GATEWAY/api/order" \
    -H 'accept: */*' -H 'Content-Type: application/json' \
    -d '{
      "customerId": "'"${1}"'",
      "items": [ { "productId": "'"${2}"'", "quantity": '"${3}"' } ]
  }'
}

get_product_by_id()    { curl -s "$GATEWAY/api/product/id/${1}"; }
get_product_by_sku()   { curl -s "$GATEWAY/api/product/sku/${1}"; }
get_products()         { curl -s "$GATEWAY/api/products"; }
get_customer_by_id()   { curl -s "$GATEWAY/api/customer/id/${1}"; }
get_customers()        { curl -s "$GATEWAY/api/customers"; }
get_order_by_id()      { curl -s "$GATEWAY/api/order/id/${1}"; }
get_orders()           { curl -s "$GATEWAY/api/orders"; }
delete_products()      { curl -sX DELETE "$GATEWAY/api/products"; }
delete_customers()     { curl -sX DELETE "$GATEWAY/api/customers"; }
delete_orders()        { curl -sX DELETE "$GATEWAY/api/orders"; }

echo "DELETE Orders"
delete_orders
echo -e "\nDELETE Customers"
delete_customers
echo -e "\nDELETE Products"
delete_products

echo -e "\n\nPOST Product 'Gaming Laptop'"
laptop_id=$(post_product "LAP-001" "Gaming Laptop" "computers" 40000 5 '{"ram":"16GB","cpu":"Ryzen 7"}' | extract_id)
echo "POST Product 'Wireless Headphones'"
headphones_id=$(post_product "HDP-001" "Wireless Headphones" "audio" 3500 50 '{"battery":"30 hours"}' | extract_id)

echo -e "\nGET Product 'Gaming Laptop' by 'id'"
get_product_by_id "${laptop_id}" | pretty

echo -e "\nGET Product 'Wireless Headphones' by 'sku'"
get_product_by_sku "HDP-001" | pretty

echo -e "\nGET Products"
get_products | pretty

echo -e "\nPOST Customer 'Juan Dela Cruz'"
juan_id=$(post_customer "Juan Dela Cruz" "juan@sample.com" | extract_id)
echo "POST Customer 'Corazon de Guzman'"
corazon_id=$(post_customer "Corazon de Guzman" "corazon@sample.com" | extract_id)

echo -e "\nGET Customer 'Juan Dela Cruz' by 'id'"
get_customer_by_id "${juan_id}" | pretty

echo -e "\nGET Customers"
get_customers | pretty

echo -e "\nPOST Order: Juan buys 1 'Gaming Laptop'"
order1_id=$(post_order "${juan_id}" "${laptop_id}" 1 | extract_id)
echo "POST Order: Corazon buys 2 'Wireless Headphones'"
order2_id=$(post_order "${corazon_id}" "${headphones_id}" 2 | extract_id)

echo -e "\nGET Order 1 by 'id'"
get_order_by_id "${order1_id}" | pretty

echo -e "\nGET Order 2 by 'id'"
get_order_by_id "${order2_id}" | pretty

echo -e "\nGET Orders"
get_orders | pretty

echo -e "\nGET Product 'Gaming Laptop' by 'id' (stock went from 5 to 4: order-service called product-service)"
get_product_by_id "${laptop_id}" | pretty