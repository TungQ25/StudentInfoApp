package com.example.taskmanagerapp.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.data.model.Category;
import com.example.taskmanagerapp.data.model.Task;
import com.example.taskmanagerapp.ui.activity.AddTaskActivity;
import com.example.taskmanagerapp.ui.activity.MainActivity;
import com.example.taskmanagerapp.ui.activity.TaskDetailActivity;
import com.example.taskmanagerapp.ui.adapter.SidebarAdapter;
import com.example.taskmanagerapp.ui.adapter.TaskAdapter;
import com.example.taskmanagerapp.ui.model.SidebarItem;
import com.example.taskmanagerapp.ui.model.SmartFilter;
import com.example.taskmanagerapp.ui.model.SystemFilter;
import com.example.taskmanagerapp.utils.ImageStorageHelper;
import com.example.taskmanagerapp.utils.PreferenceHelper;
import com.example.taskmanagerapp.viewmodel.CategoryViewModel;
import com.example.taskmanagerapp.viewmodel.TaskViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TaskFragment extends Fragment implements TaskDetailFragment.OnNavigateToFullDetailListener, MainActivity.TaskToolbarController {
    private static final String SELECT_ALL = "smart:ALL";
    private static final String SELECT_INBOX = "smart:INBOX";
    private static final int MENU_CATEGORY_EDIT = 1;
    private static final int MENU_CATEGORY_VIEW = 2;
    private static final int MENU_CATEGORY_BACKGROUND = 3;
    private static final int MENU_CATEGORY_HIDE_DETAILS = 4;
    private static final int MENU_CATEGORY_HIDE_COMPLETED = 5;
    private static final int MENU_CATEGORY_GROUP_SORT = 6;
    private static final int MENU_CATEGORY_MANAGE_SECTION = 7;
    private static final int MENU_SELECT = 8;
    private static final int MENU_FILTER_CATEGORY = 9;
    private static final int MENU_EMPTY_TRASH = 10;

    private RecyclerView rvTasks;
    private RecyclerView rvSidebar;
    private FloatingActionButton btnAddTask;
    private Button btnDeleteSelected;
    private TextView tvSelectedFilter;
    private View sidebarPanel; // Thanh sidebar
    private View sidebarScrim; // lớp phủ mờ
    private View fragmentContainer;
    private View detailScrim;
    private TaskAdapter taskAdapter;
    private SidebarAdapter sidebarAdapter;
    private TaskViewModel taskViewModel;
    private CategoryViewModel categoryViewModel;
    private ImageStorageHelper imageStorage;
    private PreferenceHelper preferenceHelper;
    private ActivityResultLauncher<Intent> addTaskLauncher;
    private ActivityResultLauncher<Intent> detailTaskLauncher;
    private final List<Task> allTasks = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private String selectedItemId = SELECT_ALL;
    private boolean categoriesLoaded = false;
    private boolean multiSelectMode = false;
    private boolean allowEmptyMultiSelectMode = false;
    private String systemCategoryFilterId = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addTaskLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                Task task = new Task(
                        data.getStringExtra("title"),
                        data.getStringExtra("description"),
                        data.getStringExtra("categoryId"),
                        data.getStringExtra("deadline"),
                        data.getBooleanExtra("completed", false),
                        data.getStringExtra("priority")
                );
                task.setWontDo(data.getBooleanExtra("wontDo", false));
                task.setImagePath(data.getStringExtra(AddTaskActivity.EXTRA_IMAGE_PATH));
                taskViewModel.addTask(task);
            }
        });

        detailTaskLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                handleTaskDetailResult(result.getData());
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvTasks = view.findViewById(R.id.rvTasks);
        btnAddTask = view.findViewById(R.id.btnAddTask);
        btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected);
        fragmentContainer = view.findViewById(R.id.fragment_container);
        detailScrim = view.findViewById(R.id.detail_scrim);
        rvSidebar = requireActivity().findViewById(R.id.rvSidebar);
        tvSelectedFilter = requireActivity().findViewById(R.id.tvSelectedFilter);
        sidebarPanel = requireActivity().findViewById(R.id.sidebarPanel);
        sidebarScrim = requireActivity().findViewById(R.id.sidebarScrim);
        preferenceHelper = new PreferenceHelper(requireContext());
        restoreSelectedFilter();
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).showTaskToolbar(currentSelectedFilterTitle(), this);
        }
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);
        imageStorage = new ImageStorageHelper(requireContext());

        setupRecyclerViews();
        setupDetailOverlay();
        setupActions();
        observeData();
        taskViewModel.syncTasks();
    }

    private void setupRecyclerViews() {
        taskAdapter = new TaskAdapter(new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task, int position) {
                if (multiSelectMode) {
                    task.setSelected(!task.isSelected());
                    allowEmptyMultiSelectMode = false;
                    taskAdapter.notifyItemChanged(position);
                    updateMultiSelectControls();
                } else {
                    showTaskDetail(task);
                }
            }

            @Override
            public void onTaskLongClick(Task task, int position) {
                toggleMultiSelectMode(task, position);
            }

            @Override
            public void onStatusChanged(Task task, boolean isCompleted) {
                task.setCompleted(isCompleted);
                taskViewModel.updateTask(task);
            }
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(taskAdapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Task task = taskAdapter.getCurrentList().get(position);
                if (shouldPermanentlyDelete(task)) {
                    taskAdapter.notifyItemChanged(position);
                }
                deleteTaskAndImage(task);
                Toast.makeText(requireContext(), shouldPermanentlyDelete(task) ? "Task permanently deleted" : "Task deleted", Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(rvTasks);

        sidebarAdapter = new SidebarAdapter(new SidebarAdapter.OnSidebarActionListener() {
            @Override
            public void onSidebarItemClick(SidebarItem item) {
                handleSidebarClick(item);
            }
            @Override
            public void onSidebarItemLongClick(SidebarItem item, View anchor) {
                if (item.getCategory() != null) showCategoryMenu(item, anchor);
            }
            @Override
            public void onSidebarOverflowClick(SidebarItem item, View anchor) {
                showCategoryMenu(item, anchor);
            }

            // TODO: Thiết lập lại setting và notification
            @Override
            public void onSidebarSettingsClick() {
                Toast.makeText(requireContext(), "Open Settings from bottom navigation", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onSidebarNotificationClick() {
                Toast.makeText(requireContext(), "Notifications are not implemented yet", Toast.LENGTH_SHORT).show();
            }
        });
        rvSidebar.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSidebar.setAdapter(sidebarAdapter);
        setupSidebarDrag();
    }

    /**
     * Thiết lập kéo thả sidebar
     */
    private void setupSidebarDrag() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                return sidebarAdapter.moveItem(from, to);
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getBindingAdapterPosition();
                return sidebarAdapter.canMove(position) ? makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) : 0;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                int position = viewHolder.getBindingAdapterPosition();
                saveSidebarOrder(sidebarAdapter.getItem(position));
            }
        }).attachToRecyclerView(rvSidebar);
    }

    /**
     * Chặn kéo lẫn qua nhóm khác
     * @param movedItem
     */
    private void saveSidebarOrder(SidebarItem movedItem) {
        if (movedItem == null) return;
        if (movedItem.getType() == SidebarItem.Type.CATEGORY) {
            categoryViewModel.updateCategoryOrders(sidebarAdapter.getOrderedMainCategories());
        } else if (movedItem.getType() == SidebarItem.Type.SMART_FILTER) {
            preferenceHelper.setSidebarSmartFilterOrder(enumOrderToPreference(sidebarAdapter.getOrderedSmartFilters()));
        } else if (movedItem.getType() == SidebarItem.Type.SYSTEM_FILTER) {
            preferenceHelper.setSidebarSystemFilterOrder(enumOrderToPreference(sidebarAdapter.getOrderedSystemFilters()));
        }
    }

    private void setupDetailOverlay() {
        getChildFragmentManager().setFragmentResultListener(TaskDetailFragment.RESULT_KEY, getViewLifecycleOwner(), (requestKey, bundle) -> {
            String resultMessage = bundle.getString(TaskDetailFragment.RESULT_MESSAGE);
            if (resultMessage != null) {
                Toast.makeText(requireContext(), resultMessage, Toast.LENGTH_SHORT).show();
            }
        });
        getChildFragmentManager().addOnBackStackChangedListener(this::updateDetailOverlayVisibility);
        if (detailScrim != null) {
            detailScrim.setOnClickListener(v -> getChildFragmentManager().popBackStack());
        }
    }

    private void setupActions() {
        btnAddTask.setOnClickListener(v -> openAddTask());
        btnDeleteSelected.setOnClickListener(v -> deleteSelectedTasks());
        if (sidebarScrim != null) sidebarScrim.setOnClickListener(v -> showSidebar(false));
        showSidebar(false);
        updateAddTaskButtonVisibility();
    }

    /**
     * Khi dữ liệu trong LiveData có giá trị mới, Fragment sẽ tự động nhận được giá trị đó và cập nhật UI
     */
    private void observeData() {
        taskViewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            allTasks.clear();
            if (tasks != null) allTasks.addAll(tasks);
            refreshUi();
        });
        categoryViewModel.getCategories().observe(getViewLifecycleOwner(), result -> {
            categoriesLoaded = true;
            categories.clear();
            if (result != null) categories.addAll(result);
            refreshUi();
        });
    }

    private void refreshUi() {
        applyTaskFilter();
        rebuildSidebar();
        updateAddTaskButtonVisibility();
    }

    /**
     * Dựng lại toàn bộ sidebar khi có thay đổi
     */
    private void rebuildSidebar() {
        if (sidebarAdapter == null) return;
        List<SidebarItem> items = rebuildSidebarItems();
        String selectedTitle = resolveSelectedTitle(items); // trả về tiêu đề (title) của item đang được chọn

        // Không có category được chọn thì giữ title cũ, không có item hợp lệ được chọn thì quay về "All"
        if (selectedTitle == null) {
            if (selectedItemId.startsWith("category:") && !categoriesLoaded) {
                selectedTitle = currentSelectedFilterTitle();
            } else {
                selectedItemId = SELECT_ALL;
                selectedTitle = "All";
                saveSelectedFilter(selectedTitle);
                items = rebuildSidebarItems();
            }
        }

        sidebarAdapter.submitItems(items);
        updateSelectedFilterTitle(selectedTitle);
    }

    /**
     * Tạo danh sách item để đưa vào sidebar
     * @return
     */
    private List<SidebarItem> rebuildSidebarItems() {
        List<SidebarItem> items = new ArrayList<>();
        items.add(SidebarItem.header(currentUserName()));

        List<Category> pinned = new ArrayList<>();
        for (Category category : categories) if (category.isPinned()) pinned.add(category);
        Collections.sort(pinned, Comparator.comparingInt(Category::getPinnedOrder));

        // TODO: sửa lại UI pinned để chỉ hiện icon
        if (!pinned.isEmpty()) {
            items.add(SidebarItem.section("Pinned"));
            for (Category category : pinned) items.add(categoryItem(category, SidebarItem.Type.PINNED_CATEGORY));
        }

        items.add(SidebarItem.section("Filters"));
        for (SmartFilter filter : orderedSmartFilters()) {
            items.add(smartItem(filter, smartFilterTitle(filter), smartFilterIcon(filter), smartFilterCount(filter)));
        }

        items.add(SidebarItem.section("Categories"));
        for (Category category : categories) items.add(categoryItem(category, SidebarItem.Type.CATEGORY));

        items.add(SidebarItem.section("System"));
        for (SystemFilter filter : orderedSystemFilters()) {
            items.add(systemItem(filter, systemFilterTitle(filter), systemFilterIcon(filter)));
        }
        items.add(SidebarItem.add());
        return items;
    }

    private SidebarItem smartItem(SmartFilter filter, String title, String icon, int count) {
        String id = "smart:" + filter.name();
        return new SidebarItem(SidebarItem.Type.SMART_FILTER, id, title, icon, count, true, id.equals(selectedItemId), null, filter, null);
    }

    private SidebarItem systemItem(SystemFilter filter, String title, String icon) {
        String id = "system:" + filter.name();
        return new SidebarItem(SidebarItem.Type.SYSTEM_FILTER, id, title, icon, 0, false, id.equals(selectedItemId), null, null, filter);
    }

    private SidebarItem categoryItem(Category category, SidebarItem.Type type) {
        String id = "category:" + category.getId();
        String icon = category.getIcon() == null || category.getIcon().isEmpty() ? "#" : category.getIcon();
        return new SidebarItem(type, id, category.getName(), icon, countCategory(category.getId()), true, id.equals(selectedItemId), category, null, null);
    }

    private List<SmartFilter> orderedSmartFilters() {
        List<SmartFilter> defaults = new ArrayList<>();
        defaults.add(SmartFilter.ALL);
        defaults.add(SmartFilter.INBOX);
        defaults.add(SmartFilter.TODAY);
        defaults.add(SmartFilter.TOMORROW);
        defaults.add(SmartFilter.NEXT_7_DAYS);
        return applySavedEnumOrder(defaults, preferenceHelper.getSidebarSmartFilterOrder(), SmartFilter.class);
    }

    private List<SystemFilter> orderedSystemFilters() {
        List<SystemFilter> defaults = new ArrayList<>();
        defaults.add(SystemFilter.COMPLETED);
        defaults.add(SystemFilter.WONT_DO);
        defaults.add(SystemFilter.TRASH);
        return applySavedEnumOrder(defaults, preferenceHelper.getSidebarSystemFilterOrder(), SystemFilter.class);
    }

    private <T extends Enum<T>> List<T> applySavedEnumOrder(List<T> defaults, String savedOrder, Class<T> enumClass) {
        List<T> ordered = new ArrayList<>();
        if (savedOrder != null && !savedOrder.trim().isEmpty()) {
            String[] names = savedOrder.split(",");
            for (String name : names) {
                try {
                    T value = Enum.valueOf(enumClass, name.trim());
                    if (defaults.contains(value) && !ordered.contains(value)) {
                        ordered.add(value);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        for (T value : defaults) {
            if (!ordered.contains(value)) {
                ordered.add(value);
            }
        }
        return ordered;
    }

    private <T extends Enum<T>> String enumOrderToPreference(List<T> values) {
        StringBuilder builder = new StringBuilder();
        for (T value : values) {
            if (value == null) continue;
            if (builder.length() > 0) builder.append(",");
            builder.append(value.name());
        }
        return builder.toString();
    }

    private String smartFilterTitle(SmartFilter filter) {
        switch (filter) {
            case INBOX:
                return "Inbox";
            case TODAY:
                return "Today";
            case TOMORROW:
                return "Tomorrow";
            case NEXT_7_DAYS:
                return "Next 7 days";
            case ALL:
            default:
                return "All";
        }
    }

    // TODO: cập nhật icon UI không dùng chữ
    private String smartFilterIcon(SmartFilter filter) {
        switch (filter) {
            case INBOX:
                return "I";
            case TODAY:
                return "T";
            case TOMORROW:
                return "M";
            case NEXT_7_DAYS:
                return "7";
            case ALL:
            default:
                return "A";
        }
    }

    private int smartFilterCount(SmartFilter filter) {
        switch (filter) {
            case INBOX:
                return countInbox();
            case TODAY:
                return countDate(dateOffset(0));
            case TOMORROW:
                return countDate(dateOffset(1));
            case NEXT_7_DAYS:
                return countNext7Days();
            case ALL:
            default:
                return countActive();
        }
    }

    private String systemFilterTitle(SystemFilter filter) {
        switch (filter) {
            case WONT_DO:
                return "Won't Do";
            case TRASH:
                return "Trash";
            case COMPLETED:
            default:
                return "Completed";
        }
    }

    // TODO: cập nhật icon UI không dùng chữ
    private String systemFilterIcon(SystemFilter filter) {
        switch (filter) {
            case WONT_DO:
                return "X";
            case TRASH:
                return "D";
            case COMPLETED:
            default:
                return "C";
        }
    }

    /**
     * Xử lý sự kiện click vào sidebar
     * @param item
     */
    private void handleSidebarClick(SidebarItem item) {
        if (item == null) return;

        // Hiển thị nút thêm Category
        if (item.getType() == SidebarItem.Type.ADD_BUTTON) {
            showAddCategoryDialog();
            return;
        }

        if (item.getType() == SidebarItem.Type.SECTION_TITLE || item.getType() == SidebarItem.Type.USER_HEADER) return;
        selectedItemId = item.getId();
        if (!isCompletedOrWontDoFilterSelected()) {
            systemCategoryFilterId = null;
        }
        clearTaskSelection();
        saveSelectedFilter(item.getTitle());
        updateSelectedFilterTitle(item.getTitle()); // cập nhật category vừa click thành đang chọn
        applyTaskFilter(); // lọc ra các task tương ứng với category vừa chọn
        rebuildSidebar(); // dựng lại sidebar sau khi click
        updateAddTaskButtonVisibility();
        showSidebar(false);
    }

    private String resolveSelectedTitle(List<SidebarItem> items) {
        for (SidebarItem item : items) {
            if (item.isSelected()) return item.getTitle();
        }
        return null;
    }

    private void restoreSelectedFilter() {
        String savedId = preferenceHelper.getSelectedTaskFilterId();
        if (savedId != null && !savedId.trim().isEmpty()) {
            selectedItemId = savedId;
        }
    }

    private String currentSelectedFilterTitle() {
        String savedTitle = preferenceHelper.getSelectedTaskFilterTitle();
        return savedTitle == null || savedTitle.trim().isEmpty() ? "All" : savedTitle;
    }

    /**
     * Lưu category/filter để restore và set lại toolbar title khi mở lại
     * @param title
     */
    private void saveSelectedFilter(String title) {
        if (preferenceHelper != null) {
            preferenceHelper.setSelectedTaskFilter(selectedItemId, title == null || title.trim().isEmpty() ? "All" : title);
        }
    }

    /**
     * Cập nhật title thanh toolbar
     * @param title
     */
    private void updateSelectedFilterTitle(String title) {
        String displayTitle = title == null || title.trim().isEmpty() ? "All" : title;
        if (tvSelectedFilter != null) tvSelectedFilter.setText(displayTitle);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setTaskToolbarTitle(displayTitle);
        }
    }

    private void openAddTask() {
        if (!canAddTaskInSelectedFilter()) {
            return;
        }

        Intent intent = new Intent(requireContext(), AddTaskActivity.class);
        if (selectedItemId.startsWith("category:")) {
            intent.putExtra("categoryId", selectedItemId.substring("category:".length()));
        }
        String defaultDeadline = defaultDeadlineForSelectedFilter(); //  tự cài ngày cho smart: today (next 7 days), tomorrow
        if (defaultDeadline != null) {
            intent.putExtra("deadline", defaultDeadline);
        }
        addTaskLauncher.launch(intent);
    }

    private String defaultDeadlineForSelectedFilter() {
        if (("smart:" + SmartFilter.TODAY.name()).equals(selectedItemId)
                || ("smart:" + SmartFilter.NEXT_7_DAYS.name()).equals(selectedItemId)) {
            return dateOffset(0);
        }
        if (("smart:" + SmartFilter.TOMORROW.name()).equals(selectedItemId)) {
            return dateOffset(1);
        }
        return null;
    }

    /**
     * Cập nhật trạng thái hiển thị của nút AddTask
     */
    private void updateAddTaskButtonVisibility() {
        if (btnAddTask != null) {
            btnAddTask.setVisibility(canAddTaskInSelectedFilter() ? View.VISIBLE : View.GONE);
        }
    }

    private boolean canAddTaskInSelectedFilter() {
        // Nếu chưa chọn filter hoặc đang chọn category thường thì hiển thị nút Add, hoặc selectedItemId không bắt đầu bằng "system:"
        if (selectedItemId == null || !selectedItemId.startsWith("system:")) {
            return true;
        }
        // Không cho thêm task trong Completed, Won’t Do, Trash
        String name = selectedItemId.substring("system:".length());
        return !SystemFilter.COMPLETED.name().equals(name)
                && !SystemFilter.WONT_DO.name().equals(name)
                && !SystemFilter.TRASH.name().equals(name);
    }

    private boolean isActive(Task task) {
        return task != null && !task.isDeleted() && !task.isCompleted() && !task.isWontDo();
    }

    private int countActive() {
        int count = 0;
        for (Task task : allTasks) if (isActive(task)) count++;
        return count;
    }

    private int countInbox() {
        int count = 0;
        for (Task task : allTasks) if (isActive(task) && isBlank(task.getCategoryId())) count++;
        return count;
    }

    private int countCategory(String categoryId) {
        int count = 0;
        for (Task task : allTasks) if (isActive(task) && categoryId.equals(task.getCategoryId())) count++;
        return count;
    }

    private int countDate(String date) {
        int count = 0;
        for (Task task : allTasks) if (isActive(task) && date.equals(task.getDeadline())) count++;
        return count;
    }

    private int countNext7Days() {
        int count = 0;
        String start = dateOffset(0);
        String end = dateOffset(6);
        for (Task task : allTasks) {
            String deadline = task.getDeadline();
            if (isActive(task) && deadline != null && deadline.compareTo(start) >= 0 && deadline.compareTo(end) <= 0) count++;
        }
        return count;
    }

    private String dateOffset(int offsetDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, offsetDays);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
    }

    /**
     * Áp dụng filter vào cho task (lọc task với category) và add vào Adapter
     */
    private void applyTaskFilter() {
        if (taskAdapter == null) return;
        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (matchesSelectedFilter(task)) filteredTasks.add(task);
        }
        taskAdapter.submitList(filteredTasks); // thêm list task vào adapter
        updateMultiSelectControls(filteredTasks);
    }

    /**
     * Lọc task theo category
     * @param task
     * @return
     */
    private boolean matchesSelectedFilter(Task task) {
        if (task == null) return false;
        if (selectedItemId.startsWith("category:")) {
            String categoryId = selectedItemId.substring("category:".length());
            return isActive(task) && categoryId.equals(task.getCategoryId());
        }

        if (selectedItemId.startsWith("system:")) {
            String name = selectedItemId.substring("system:".length());
            if (SystemFilter.COMPLETED.name().equals(name))
                return !task.isDeleted() && task.isCompleted() && matchesSystemCategoryFilter(task);
            if (SystemFilter.WONT_DO.name().equals(name))
                return !task.isDeleted() && task.isWontDo() && matchesSystemCategoryFilter(task);
            if (SystemFilter.TRASH.name().equals(name))
                return task.isDeleted();
        }

        if (SELECT_INBOX.equals(selectedItemId))
            return isActive(task) && isBlank(task.getCategoryId());
        if (("smart:TODAY").equals(selectedItemId))
            return isActive(task) && dateOffset(0).equals(task.getDeadline());
        if (("smart:TOMORROW").equals(selectedItemId))
            return isActive(task) && dateOffset(1).equals(task.getDeadline());
        if (("smart:NEXT_7_DAYS").equals(selectedItemId)) {
            String deadline = task.getDeadline();
            return isActive(task) && deadline != null && deadline.compareTo(dateOffset(0)) >= 0 && deadline.compareTo(dateOffset(6)) <= 0;
        }

        return isActive(task);
    }

    /**
     * Lọc task ở completed hoặc wont-do khớp với category phụ
     * @param task
     * @return
     */
    private boolean matchesSystemCategoryFilter(Task task) {
        // Task thuộc category nào cũng được hiện, miễn là đã Completed
        if (systemCategoryFilterId == null) {
            return true;
        }
        // Lọc các task không có category, tức là Inbox (systemCategoryFilterId = "")
        if (systemCategoryFilterId.isEmpty()) {
            return isBlank(task.getCategoryId());
        }
        // Nếu systemCategoryFilterId có giá trị cụ thể thì task phải thuộc đúng category đó
        return systemCategoryFilterId.equals(task.getCategoryId());
    }

    private void showToolbarActionMenu(View anchor) {
        if (anchor == null) return;
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        if (isTrashFilterSelected()) {
            menu.getMenu().add(0, MENU_SELECT, 0, "Select");
            menu.getMenu().add(0, MENU_EMPTY_TRASH, 1, "Empty trash");
        } else if (isCompletedOrWontDoFilterSelected()) {
            menu.getMenu().add(0, MENU_SELECT, 0, "Select");
            menu.getMenu().add(0, MENU_FILTER_CATEGORY, 1, "Filter");
        } else if (isCategoryFilterSelected()) {
            menu.getMenu().add(0, MENU_CATEGORY_EDIT, 0, "Edit");
            menu.getMenu().add(0, MENU_CATEGORY_VIEW, 1, "View");
            menu.getMenu().add(0, MENU_CATEGORY_BACKGROUND, 2, "Background");
            menu.getMenu().add(0, MENU_CATEGORY_HIDE_DETAILS, 3, "Hide Details");
            menu.getMenu().add(0, MENU_CATEGORY_HIDE_COMPLETED, 4, "Hide Completed");
            menu.getMenu().add(0, MENU_CATEGORY_GROUP_SORT, 5, "Group & Sort");
            menu.getMenu().add(0, MENU_CATEGORY_MANAGE_SECTION, 6, "Manage Section");
            menu.getMenu().add(0, MENU_SELECT, 7, "Select");
        } else {
            menu.getMenu().add(0, MENU_SELECT, 0, "Select");
            menu.getMenu().add(0, MENU_CATEGORY_GROUP_SORT, 1, "Group & Sort");
        }
        menu.setOnMenuItemClickListener(item -> handleToolbarMenuItem(item.getItemId()));
        menu.show();
    }

    private boolean handleToolbarMenuItem(int itemId) {
        if (itemId == MENU_SELECT) {
            enterMultiSelectMode();
            return true;
        }
        if (itemId == MENU_FILTER_CATEGORY) {
            showSystemCategoryFilterDialog();
            return true;
        }
        if (itemId == MENU_EMPTY_TRASH) {
            confirmEmptyTrash();
            return true;
        }
        if (itemId == MENU_CATEGORY_EDIT) {
            Category category = currentSelectedCategory();
            if (category != null) {
                showRenameCategoryDialog(category);
            }
            return true;
        }

        Toast.makeText(requireContext(), "This action is not implemented yet", Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * Chuyển sang chế độ chọn nhiều
     */
    private void enterMultiSelectMode() {
        multiSelectMode = true;
        allowEmptyMultiSelectMode = true;
        if (taskAdapter != null) {
            for (Task task : taskAdapter.getCurrentList()) {
                task.setSelected(false);
            }
            taskAdapter.notifyDataSetChanged();
        }
        updateMultiSelectControls();
        Toast.makeText(requireContext(), "Select tasks", Toast.LENGTH_SHORT).show();
    }

    /**
     * Tắt chế độ chọn nhiều và xóa trạng thái chọn của các task
     */
    private void clearTaskSelection() {
        multiSelectMode = false;
        allowEmptyMultiSelectMode = false;
        for (Task task : allTasks) {
            task.setSelected(false);
        }
        if (taskAdapter != null) {
            taskAdapter.notifyDataSetChanged();
        }
        updateMultiSelectControls();
    }

    /**
     * Gán dữ liệu cho systemCategoryFilterId dựa trên lựa chọn của người dùng trong dialog.
     */
    private void showSystemCategoryFilterDialog() {
        List<String> labels = new ArrayList<>();
        List<String> categoryIds = new ArrayList<>();
        labels.add("All categories");
        categoryIds.add(null);
        labels.add("Inbox");
        categoryIds.add("");
        for (Category category : categories) {
            labels.add(category.getName());
            categoryIds.add(category.getId());
        }

        CharSequence[] items = labels.toArray(new CharSequence[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle("Filter by category")
                .setItems(items, (dialog, which) -> {
                    systemCategoryFilterId = categoryIds.get(which); // Gán dữ liệu cho systemCategoryFilterId
                    applyTaskFilter();
                    Toast.makeText(requireContext(), "Filtered by " + labels.get(which), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmEmptyTrash() {
        int trashCount = 0;
        for (Task task : allTasks) {
            if (task.isDeleted()) trashCount++;
        }
        if (trashCount == 0) {
            Toast.makeText(requireContext(), "Trash is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Empty trash")
                .setMessage("Delete all tasks in Trash permanently?")
                .setPositiveButton("Empty trash", (dialog, which) -> taskViewModel.emptyTrash())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Category currentSelectedCategory() {
        if (!isCategoryFilterSelected()) return null;
        String categoryId = selectedItemId.substring("category:".length());
        for (Category category : categories) {
            if (categoryId.equals(category.getId())) return category;
        }
        return null;
    }

    private boolean isCategoryFilterSelected() {
        return selectedItemId != null && selectedItemId.startsWith("category:");
    }

    private boolean isCompletedOrWontDoFilterSelected() {
        return ("system:" + SystemFilter.COMPLETED.name()).equals(selectedItemId)
                || ("system:" + SystemFilter.WONT_DO.name()).equals(selectedItemId);
    }

    private void showCategoryMenu(SidebarItem item, View anchor) {
        Category category = item.getCategory();
        if (category == null) return;
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(category.isPinned() ? "Unpin" : "Pin");
        menu.getMenu().add("Rename");
        menu.getMenu().add("Delete");
        menu.setOnMenuItemClickListener(menuItem -> {
            String title = menuItem.getTitle().toString();
            if ("Rename".equals(title)) {
                showRenameCategoryDialog(category);
            } else if ("Delete".equals(title)) {
                confirmDeleteCategory(category);
            } else {
                categoryViewModel.togglePinned(category);
            }
            return true;
        });
        menu.show();
    }

    // TODO: cập nhật UI AddCategory
    private void showAddCategoryDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, 0);
        EditText edtName = new EditText(requireContext());
        edtName.setHint("Name");
        EditText edtIcon = new EditText(requireContext());
        edtIcon.setHint("Icon");
        layout.addView(edtName);
        layout.addView(edtIcon);
        new AlertDialog.Builder(requireContext())
                .setTitle("Add category")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> categoryViewModel.addCategory(edtName.getText().toString(), edtIcon.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // TODO: cập nhật UI rename thành edit và thêm các chọn màu, icon
    private void showRenameCategoryDialog(Category category) {
        EditText input = new EditText(requireContext());
        input.setText(category.getName());
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(requireContext())
                .setTitle("Rename category")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> categoryViewModel.renameCategory(category, input.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // TODO: cập nhật UI thông báo delete category
    private void confirmDeleteCategory(Category category) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete category")
                .setMessage("All tasks in this list will also be deleted.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (("category:" + category.getId()).equals(selectedItemId)) {
                        selectedItemId = "system:" + SystemFilter.TRASH.name();
                        saveSelectedFilter("Trash");
                        updateSelectedFilterTitle("Trash");
                    }
                    categoryViewModel.deleteCategory(category);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSidebar(boolean show) {
        if (sidebarPanel == null) return;
        sidebarPanel.setVisibility(show ? View.VISIBLE : View.GONE);
        if (sidebarScrim != null) sidebarScrim.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Bật/tắt chế độ chọn nhiều
     * @param task
     * @param position
     */
    private void toggleMultiSelectMode(Task task, int position) {
        multiSelectMode = true;
        allowEmptyMultiSelectMode = false;
        task.setSelected(true);
        taskAdapter.notifyItemChanged(position);
        updateMultiSelectControls();
    }

    private void updateMultiSelectControls() {
        if (taskAdapter == null) {
            return;
        }
        updateMultiSelectControls(taskAdapter.getCurrentList());
    }

    private void updateMultiSelectControls(List<Task> visibleTasks) {
        if (btnDeleteSelected == null) return;
        if (!multiSelectMode) {
            btnDeleteSelected.setVisibility(View.GONE);
            return;
        }
        for (Task task : visibleTasks) {
            if (task.isSelected()) {
                allowEmptyMultiSelectMode = false;
                btnDeleteSelected.setVisibility(View.VISIBLE);
                return;
            }
        }
        if (!allowEmptyMultiSelectMode) {
            multiSelectMode = false;
        }
        btnDeleteSelected.setVisibility(View.GONE);
    }

    private void deleteSelectedTasks() {
        List<Task> currentList = taskAdapter.getCurrentList();
        int deletedCount = 0;
        for (Task task : currentList) {
            if (task.isSelected()) {
                deleteTaskAndImage(task);
                deletedCount++;
            }
        }
        if (deletedCount == 0) {
            Toast.makeText(requireContext(), "No tasks selected", Toast.LENGTH_SHORT).show();
        }
        clearTaskSelection();
    }

    private void deleteTaskAndImage(Task task) {
        if (task == null) return;
        String img = task.getImagePath();
        if (img != null && !img.isEmpty()) imageStorage.deleteImage(img);
        if (shouldPermanentlyDelete(task)) {
            taskViewModel.permanentlyDeleteTask(task.getId());
        } else {
            taskViewModel.deleteTask(task.getId());
        }
    }

    private boolean shouldPermanentlyDelete(Task task) {
        return task != null && task.isDeleted();
    }

    private boolean isTrashFilterSelected() {
        return ("system:" + SystemFilter.TRASH.name()).equals(selectedItemId);
    }

    private Task findTaskById(String taskId) {
        if (taskId == null) return null;
        for (Task task : allTasks) if (taskId.equals(task.getId())) return task;
        return null;
    }

    private boolean isTwoPane() {
        return getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    private void showTaskDetail(Task task) {
        TaskDetailFragment fragment = TaskDetailFragment.newInstance(task);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.VISIBLE);
        if (detailScrim != null) detailScrim.setVisibility(isTwoPane() ? View.GONE : View.VISIBLE);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onNavigateToFullDetail(Task task) {
        if (task == null) {
            return;
        }
        getChildFragmentManager().popBackStack();
        int position = findTaskListPosition(task);
        openTaskDetailActivity(task, position >= 0 ? position : 0);
    }

    private int findTaskListPosition(Task task) {
        if (task == null || task.getId() == null) {
            return -1;
        }
        List<Task> list = taskAdapter.getCurrentList();
        for (int i = 0; i < list.size(); i++) {
            if (task.getId().equals(list.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    private void openTaskDetailActivity(Task task, int position) {
        Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
        intent.putExtra("id", task.getId());
        intent.putExtra("title", task.getTitle());
        intent.putExtra("description", task.getDescription());
        intent.putExtra("categoryId", task.getCategoryId());
        intent.putExtra("categoryName", getCategoryName(task.getCategoryId()));
        intent.putExtra("priority", task.getPriority());
        intent.putExtra("deadline", task.getDeadline());
        intent.putExtra("completed", task.isCompleted());
        intent.putExtra("wontDo", task.isWontDo());
        intent.putExtra("position", position);
        intent.putExtra(AddTaskActivity.EXTRA_IMAGE_PATH, task.getImagePath());
        detailTaskLauncher.launch(intent);
    }

    private void handleTaskDetailResult(Intent data) {
        String taskId = data.getStringExtra("id");
        if (taskId == null) {
            return;
        }

        if (data.getBooleanExtra("deleted", false)) {
            Task existing = findTaskById(taskId);
            if (existing != null) {
                deleteTaskAndImage(existing);
            } else {
                String img = data.getStringExtra(AddTaskActivity.EXTRA_IMAGE_PATH);
                if (img != null) imageStorage.deleteImage(img);
                if (isTrashFilterSelected()) {
                    taskViewModel.permanentlyDeleteTask(taskId);
                } else {
                    taskViewModel.deleteTask(taskId);
                }
            }
            return;
        }

        if (data.getBooleanExtra("updated", false)) {
            String newImagePath = data.getStringExtra(AddTaskActivity.EXTRA_IMAGE_PATH);
            Task existing = findTaskById(taskId);
            if (existing != null) {
                String oldImagePath = existing.getImagePath();
                if (oldImagePath != null && !oldImagePath.equals(newImagePath)) {
                    imageStorage.deleteImage(oldImagePath);
                }
            }

            Task updatedTask = new Task(
                    taskId,
                    data.getStringExtra("title"),
                    data.getStringExtra("description"),
                    data.getStringExtra("categoryId"),
                    data.getStringExtra("deadline"),
                    data.getBooleanExtra("completed", false),
                    data.getStringExtra("priority"));
            updatedTask.setWontDo(data.getBooleanExtra("wontDo", false));
            updatedTask.setImagePath(newImagePath);
            taskViewModel.updateTask(updatedTask);
        }
    }

    private String getCategoryName(String categoryId) {
        if (isBlank(categoryId)) return "Inbox";
        for (Category category : categories) if (categoryId.equals(category.getId())) return category.getName();
        return "Inbox";
    }

    private String currentUserName() {
        String username = preferenceHelper.getAuthUsername();
        return username == null || username.trim().isEmpty() ? "User" : username;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void updateDetailOverlayVisibility() {
        boolean hasDetail = getChildFragmentManager().getBackStackEntryCount() > 0;
        if (fragmentContainer != null) fragmentContainer.setVisibility(hasDetail ? View.VISIBLE : View.GONE);
        if (detailScrim != null) detailScrim.setVisibility(!isTwoPane() && hasDetail ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onToggleSidebarRequested() {
        showSidebar(true);
    }

    @Override
    public void onToolbarMoreRequested(View anchor) {
        showToolbarActionMenu(anchor);
    }

    @Override
    public void onDestroyView() {
        showSidebar(false);
        getChildFragmentManager().clearFragmentResultListener(TaskDetailFragment.RESULT_KEY);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).clearTaskToolbarController(this);
        }
        super.onDestroyView();
        rvTasks = null;
        rvSidebar = null;
        btnAddTask = null;
        btnDeleteSelected = null;
        tvSelectedFilter = null;
        sidebarPanel = null;
        sidebarScrim = null;
        fragmentContainer = null;
        detailScrim = null;
        taskAdapter = null;
        sidebarAdapter = null;
    }
}
