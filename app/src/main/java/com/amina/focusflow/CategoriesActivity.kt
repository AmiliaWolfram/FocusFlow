package com.amina.focusflow

// Категории использую и в задачах, и в статистике, поэтому храню их отдельно.
import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class CategoriesActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b);
        render()
    }
    private fun render() {
        val(sc, body) = Ui.screen(this);
        Ui.header(body,"Categories","Build your own life areas.");
        val tasks = DataManager.loadTasks(this);
        DataManager.categories(this).forEachIndexed {
            i, c -> val card = Ui.card(this, listOf(Ui.sage, Ui.surface, Ui.blue)[i%3]);
            val r = Ui.row(this);
            r.addView(Ui.icon(this, Ui.categoryIcon(c.name), Ui.green, 24));
            r.addView(Ui.spacer(this, 12));
            r.addView(Ui.text(this, c.name, 17f, true), LinearLayout.LayoutParams(0, -2, 1f));
            r.addView(Ui.chip(this,"${tasks.count{it.categoryId==c.id||it.category==c.name}} tasks", Ui.surface));
            card.addView(r);
            card.addView(Ui.text(this,"Hold to delete", 11f, false, Ui.muted), Ui.lp(Ui.dp(this, 7)));
            card.setOnLongClickListener {
                delete(c);
                true
            }
            ;
            body.addView(card, Ui.lp(Ui.dp(this, 10)))
        }
        ;
        val add = Ui.button(this,"New category", Ui.green);
        add.setOnClickListener {
            create()
        }
        ;
        body.addView(add, Ui.lp(Ui.dp(this, 16)));
        setContentView(sc)
    }
    private fun create() {
        val input = EditText(this).apply {
            hint ="e.g. Hiking"
        }
        ;
        AlertDialog.Builder(this).setTitle("New category").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Create") {
            _, _ -> val n = input.text.toString().trim();
            if (n.isNotBlank()) {
                val x = DataManager.categories(this);
                x.add(Category(name = n, icon ="category"));
                DataManager.saveCategories(this, x);
                render()
            }
        }
        .show()
    }
    private fun delete(c: Category) {
        AlertDialog.Builder(this).setTitle("Delete ${c.name}?").setNegativeButton("Cancel", null).setPositiveButton("Delete") {
            _, _ -> val x = DataManager.categories(this);
            x.removeAll {
                it.id == c.id
            }
            ;
            DataManager.saveCategories(this, x);
            render()
        }
        .show()
    }
}
