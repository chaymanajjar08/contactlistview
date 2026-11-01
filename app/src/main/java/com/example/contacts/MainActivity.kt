package com.example.contacts

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    // Une liste modifiable : l'adaptateur affiche ces mêmes données.
    private val contacts = mutableListOf(
        "Chayma Najjar", "Ines Lanouar", "Mohamed BenMimoun", "Nadia Jaziri",
        "Aymen Mansour", "Amira Chaabane", "Karim Bacouche", "Nour Abri"
    )
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var etContact: EditText
    private lateinit var lvContacts: ListView
    private var positionSelectionnee = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Android 15 : garder les commandes à l'écart des barres système et du clavier.
        val root = findViewById<android.view.View>(R.id.rootContacts)
        val padding = root.paddingLeft
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(padding + bars.left, padding + bars.top,
                padding + bars.right, padding + maxOf(bars.bottom, keyboard.bottom))
            insets
        }
        ViewCompat.requestApplyInsets(root)

        etContact = findViewById(R.id.etContact)
        lvContacts = findViewById(R.id.lvContacts)
        savedInstanceState?.getStringArrayList(KEY_CONTACTS)?.let {
            contacts.clear()
            contacts.addAll(it)
        }
        trierContacts()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_activated_1, contacts)
        lvContacts.adapter = adapter

        lvContacts.setOnItemClickListener { _, _, position, _ -> selectionner(position) }
        lvContacts.setOnItemLongClickListener { _, _, position, _ ->
            confirmerSuppression(contacts[position])
            true
        }
        findViewById<Button>(R.id.btnModifier).setOnClickListener { modifierContact() }
        findViewById<Button>(R.id.btnAjouter).setOnClickListener { ajouterContact() }
        etContact.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                modifierContact()
                true
            } else false
        }

        // Le contenu de la liste ET la sélection survivent à une rotation.
        positionSelectionnee = savedInstanceState?.getInt(KEY_SELECTION, -1) ?: -1
        if (positionSelectionnee in contacts.indices) {
            lvContacts.setItemChecked(positionSelectionnee, true)
        } else positionSelectionnee = -1
        savedInstanceState?.getString(KEY_DRAFT)?.let { etContact.setText(it) }
    }

    private fun selectionner(position: Int) {
        positionSelectionnee = position
        lvContacts.setItemChecked(position, true)
        etContact.error = null
        etContact.setText(contacts[position])
        etContact.setSelection(etContact.text.length)
        etContact.requestFocus()
    }

    private fun nomValide(positionIgnoree: Int = -1): String? {
        val nom = etContact.text.toString().trim()
        if (nom.isEmpty()) {
            etContact.error = getString(R.string.msg_nom_vide)
            return null
        }
        if (contacts.indices.any { it != positionIgnoree && contacts[it].equals(nom, ignoreCase = true) }) {
            etContact.error = getString(R.string.msg_doublon)
            return null
        }
        etContact.error = null
        return nom
    }

    private fun modifierContact() {
        if (positionSelectionnee !in contacts.indices) {
            Toast.makeText(this, R.string.msg_aucune_selection, Toast.LENGTH_SHORT).show()
            return
        }
        val nom = nomValide(positionSelectionnee) ?: return
        contacts[positionSelectionnee] = nom
        actualiser(nom)
        Toast.makeText(this, getString(R.string.msg_contact_modifie, nom), Toast.LENGTH_SHORT).show()
    }

    private fun ajouterContact() {
        val nom = nomValide() ?: return
        contacts.add(nom)
        actualiser(nom)
        Toast.makeText(this, getString(R.string.msg_contact_ajoute, nom), Toast.LENGTH_SHORT).show()
    }

    private fun confirmerSuppression(nom: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.titre_suppression)
            .setMessage(getString(R.string.confirmer_suppression, nom))
            .setNegativeButton(R.string.annuler, null)
            .setPositiveButton(R.string.supprimer) { _, _ ->
                val selection = contacts.getOrNull(positionSelectionnee)
                contacts.remove(nom)
                actualiser(if (selection == nom) null else selection)
                Toast.makeText(this, getString(R.string.msg_contact_supprime, nom), Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun trierContacts() {
        contacts.sortWith(String.CASE_INSENSITIVE_ORDER)
    }

    private fun actualiser(nomSelectionne: String?) {
        trierContacts()
        // Le point clé du TP : prévenir l'adaptateur après une modification.
        adapter.notifyDataSetChanged()
        lvContacts.clearChoices()
        positionSelectionnee = contacts.indexOf(nomSelectionne)
        if (positionSelectionnee >= 0) {
            lvContacts.setItemChecked(positionSelectionnee, true)
            lvContacts.smoothScrollToPosition(positionSelectionnee)
            etContact.setText(nomSelectionne)
            etContact.setSelection(etContact.text.length)
        } else etContact.text.clear()
        etContact.error = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(KEY_CONTACTS, ArrayList(contacts))
        outState.putInt(KEY_SELECTION, positionSelectionnee)
        outState.putString(KEY_DRAFT, etContact.text.toString())
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_CONTACTS = "contacts"
        private const val KEY_SELECTION = "selection"
        private const val KEY_DRAFT = "brouillon"
    }
}
