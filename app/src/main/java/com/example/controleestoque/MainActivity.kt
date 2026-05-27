package com.example.controleestoque

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ControleEstoqueApp()
        }
    }
}

private enum class Screen(val label: String) {
    Dashboard("Painel"),
    Products("Produtos"),
    Movements("Movimentacoes")
}

private enum class MovementType(val label: String) {
    Entry("Entrada"),
    Exit("Saida")
}

private data class Product(
    val id: Int,
    val name: String,
    val sku: String,
    val quantity: Int,
    val minQuantity: Int,
    val maxQuantity: Int,
    val unitPrice: Double
)

private data class StockMovement(
    val id: Int,
    val productId: Int,
    val productName: String,
    val type: MovementType,
    val quantity: Int,
    val createdAt: LocalDateTime
)

@Composable
private fun ControleEstoqueApp() {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF2E6F57),
            secondary = Color(0xFF425D8A),
            tertiary = Color(0xFF8A5A44),
            background = Color(0xFFF6F8F7),
            surface = Color.White
        )
    ) {
        var isAuthenticated by remember { mutableStateOf(false) }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isAuthenticated) {
                InventoryHome(onLogout = { isAuthenticated = false })
            } else {
                LoginScreen(onLoginSuccess = { isAuthenticated = true })
            }
        }
    }
}

@Composable
private fun LoginScreen(onLoginSuccess: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Controle de Estoque",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Acesse com admin / admin",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = user,
                    onValueChange = {
                        user = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Usuario") }
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Senha") },
                    visualTransformation = PasswordVisualTransformation()
                )
                error?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        if (user == "admin" && password == "admin") {
                            onLoginSuccess()
                        } else {
                            error = "Usuario ou senha invalidos."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Entrar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryHome(onLogout: () -> Unit) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedScreen by remember { mutableStateOf(Screen.Dashboard) }
    var nextProductId by remember { mutableIntStateOf(4) }
    var nextMovementId by remember { mutableIntStateOf(4) }
    val products = remember {
        mutableStateListOf(
            Product(1, "Notebook Dell", "NTB-001", 8, 5, 20, 3450.0),
            Product(2, "Mouse sem fio", "MOU-014", 35, 10, 80, 89.9),
            Product(3, "Teclado mecanico", "TEC-022", 16, 8, 40, 249.9)
        )
    }
    val movements = remember {
        mutableStateListOf(
            StockMovement(1, 1, "Notebook Dell", MovementType.Entry, 10, LocalDateTime.now().minusDays(2)),
            StockMovement(2, 2, "Mouse sem fio", MovementType.Entry, 40, LocalDateTime.now().minusDays(1)),
            StockMovement(3, 1, "Notebook Dell", MovementType.Exit, 2, LocalDateTime.now().minusHours(5))
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Controle de Estoque",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Screen.entries.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.label) },
                        selected = selectedScreen == screen,
                        onClick = {
                            selectedScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                NavigationDrawerItem(
                    label = { Text("Sair") },
                    selected = false,
                    onClick = onLogout,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(selectedScreen.label) },
                    navigationIcon = {
                        TextButton(onClick = { scope.launch { drawerState.open() } }) {
                            Text("Menu")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            when (selectedScreen) {
                Screen.Dashboard -> DashboardScreen(products, movements, padding)
                Screen.Products -> ProductsScreen(
                    products = products,
                    contentPadding = padding,
                    onSave = { editedProduct ->
                        val index = products.indexOfFirst { it.id == editedProduct.id }
                        if (index >= 0) {
                            products[index] = editedProduct
                        } else {
                            products.add(editedProduct.copy(id = nextProductId++))
                        }
                    }
                )
                Screen.Movements -> MovementsScreen(
                    products = products,
                    movements = movements,
                    contentPadding = padding,
                    onAddMovement = { product, type, quantity ->
                        val productIndex = products.indexOfFirst { it.id == product.id }
                        if (productIndex >= 0) {
                            val current = products[productIndex]
                            val newQuantity = when (type) {
                                MovementType.Entry -> current.quantity + quantity
                                MovementType.Exit -> current.quantity - quantity
                            }
                            products[productIndex] = current.copy(quantity = newQuantity)
                            movements.add(
                                0,
                                StockMovement(
                                    id = nextMovementId++,
                                    productId = current.id,
                                    productName = current.name,
                                    type = type,
                                    quantity = quantity,
                                    createdAt = LocalDateTime.now()
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    products: List<Product>,
    movements: List<StockMovement>,
    contentPadding: PaddingValues
) {
    val totalProducts = products.size
    val totalItems = products.sumOf { it.quantity }
    val totalValue = products.sumOf { it.quantity * it.unitPrice }
    val lowStockProducts = products.filter { it.quantity <= it.minQuantity }
    val sortedMovements = movements.sortedByDescending { it.createdAt }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SummaryCard("Produtos cadastrados", totalProducts.toString())
        }
        item {
            SummaryCard("Itens em estoque", totalItems.toString())
        }
        item {
            SummaryCard("Valor total em estoque", money(totalValue))
        }
        if (lowStockProducts.isNotEmpty()) {
            item {
                Text(
                    text = "Alertas de estoque minimo",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(lowStockProducts) { product ->
                LowStockCard(product)
            }
        }
        item {
            Text(
                text = "Movimentacoes",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (sortedMovements.isEmpty()) {
            item {
                Text(
                    text = "Nenhuma movimentacao registrada.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(sortedMovements) { movement ->
            MovementRow(movement)
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LowStockCard(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8A4B00)
            )
            Text(
                text = "Estoque atual: ${product.quantity} | minimo: ${product.minQuantity}",
                color = Color(0xFF8A4B00)
            )
        }
    }
}

@Composable
private fun ProductsScreen(
    products: List<Product>,
    contentPadding: PaddingValues,
    onSave: (Product) -> Unit
) {
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                editingProduct = null
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cadastrar produto")
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(products) { product ->
                ProductCard(
                    product = product,
                    onEdit = {
                        editingProduct = product
                        showDialog = true
                    }
                )
            }
        }
    }

    if (showDialog) {
        ProductDialog(
            product = editingProduct,
            onDismiss = { showDialog = false },
            onSave = {
                onSave(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ProductCard(product: Product, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(product.name, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onEdit) {
                    Text("Editar")
                }
            }
            Text("SKU: ${product.sku}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Quantidade: ${product.quantity}")
            Text("Estoque minimo: ${product.minQuantity}")
            Text("Estoque maximo: ${product.maxQuantity}")
            Text("Valor unitario: ${money(product.unitPrice)}")
        }
    }
}

@Composable
private fun ProductDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember(product) { mutableStateOf(product?.name.orEmpty()) }
    var sku by remember(product) { mutableStateOf(product?.sku.orEmpty()) }
    var quantity by remember(product) { mutableStateOf(product?.quantity?.toString().orEmpty()) }
    var minQuantity by remember(product) { mutableStateOf(product?.minQuantity?.toString().orEmpty()) }
    var maxQuantity by remember(product) { mutableStateOf(product?.maxQuantity?.toString().orEmpty()) }
    var price by remember(product) { mutableStateOf(product?.unitPrice?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Novo produto" else "Editar produto") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.onlyDigits() },
                    label = { Text("Quantidade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minQuantity,
                        onValueChange = { minQuantity = it.onlyDigits() },
                        modifier = Modifier.weight(1f),
                        label = { Text("Minimo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxQuantity,
                        onValueChange = { maxQuantity = it.onlyDigits() },
                        modifier = Modifier.weight(1f),
                        label = { Text("Maximo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.replace(',', '.') },
                    label = { Text("Valor unitario") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedQuantity = quantity.toIntOrNull()
                    val parsedMinQuantity = minQuantity.toIntOrNull()
                    val parsedMaxQuantity = maxQuantity.toIntOrNull()
                    val parsedPrice = price.toDoubleOrNull()
                    when {
                        name.isBlank() || sku.isBlank() || parsedQuantity == null ||
                            parsedMinQuantity == null || parsedMaxQuantity == null || parsedPrice == null -> {
                            error = "Preencha todos os campos corretamente."
                        }
                        parsedMinQuantity > parsedMaxQuantity -> {
                            error = "O minimo nao pode ser maior que o maximo."
                        }
                        parsedQuantity > parsedMaxQuantity -> {
                            error = "A quantidade atual nao pode passar do maximo."
                        }
                        else -> {
                            onSave(
                                Product(
                                    id = product?.id ?: 0,
                                    name = name.trim(),
                                    sku = sku.trim(),
                                    quantity = parsedQuantity,
                                    minQuantity = parsedMinQuantity,
                                    maxQuantity = parsedMaxQuantity,
                                    unitPrice = parsedPrice
                                )
                            )
                        }
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun MovementsScreen(
    products: List<Product>,
    movements: List<StockMovement>,
    contentPadding: PaddingValues,
    onAddMovement: (Product, MovementType, Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = products.isNotEmpty()
        ) {
            Text("Registrar movimentacao")
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(movements.sortedByDescending { it.createdAt }) { movement ->
                MovementRow(movement)
            }
        }
    }

    if (showDialog) {
        MovementDialog(
            products = products,
            onDismiss = { showDialog = false },
            onSave = { product, type, quantity ->
                onAddMovement(product, type, quantity)
                showDialog = false
            }
        )
    }
}

@Composable
private fun MovementDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onSave: (Product, MovementType, Int) -> Unit
) {
    var selectedProduct by remember { mutableStateOf(products.first()) }
    var type by remember { mutableStateOf(MovementType.Entry) }
    var quantity by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova movimentacao") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Produto")
                products.forEach { product ->
                    OutlinedButton(
                        onClick = { selectedProduct = product },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedProduct.id == product.id) "${product.name} selecionado" else product.name)
                    }
                }
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MovementType.entries.forEach { movementType ->
                        if (type == movementType) {
                            Button(onClick = { type = movementType }) {
                                Text(movementType.label)
                            }
                        } else {
                            OutlinedButton(onClick = { type = movementType }) {
                                Text(movementType.label)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it.onlyDigits()
                        error = null
                    },
                    label = { Text("Quantidade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Text("Estoque atual: ${selectedProduct.quantity}")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedQuantity = quantity.toIntOrNull()
                    when {
                        parsedQuantity == null || parsedQuantity <= 0 -> {
                            error = "Informe uma quantidade valida."
                        }
                        type == MovementType.Exit && parsedQuantity > selectedProduct.quantity -> {
                            error = "Saida maior que o estoque disponivel."
                        }
                        type == MovementType.Entry && selectedProduct.quantity + parsedQuantity > selectedProduct.maxQuantity -> {
                            error = "Entrada ultrapassa o estoque maximo."
                        }
                        else -> onSave(selectedProduct, type, parsedQuantity)
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun MovementRow(movement: StockMovement) {
    val color = if (movement.type == MovementType.Entry) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(movement.productName, fontWeight = FontWeight.Bold)
                Text(
                    "${movement.type.label} de ${movement.quantity} un. em ${dateTime(movement.createdAt)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (movement.type == MovementType.Entry) "+${movement.quantity}" else "-${movement.quantity}",
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun String.onlyDigits(): String = filter { it.isDigit() }

private fun money(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}

private fun dateTime(value: LocalDateTime): String {
    return value.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
}
