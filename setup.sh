echo "Setup Confluent Cloud and all Gradle Builds for Demo"
echo "-------------------------------------"
echo ""

echo "Initializing Confluent Cloud Environment..."
cd cc-terraform
terraform init
terraform plan -out "tfplan"
terraform apply "tfplan"

echo "Confluent Cloud Environment Complete."

echo ""
echo "-------------------------------------"
echo "Exporting Confluent Cloud Connection Properties for later use..."
terraform output -json \
 | jq -r 'to_entries | map( {key: .key|tostring|split("_")|join("."), value: .value} ) | map("\(.key)=\(.value.value)") | .[]' \
 | while read -r line ; do echo "$line"; done > ../shared/src/main/resources/confluent.properties

cd ../

echo "Setup Complete!"