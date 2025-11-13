/**
 * Enhanced Table Utilities
 * Provides sorting, filtering, and pagination for HTML tables
 *
 * @author ajsinha@gmail.com
 * @copyright 2025 Ash Sinha. All rights reserved.
 */

class EnhancedTable {
    constructor(tableId, options = {}) {
        this.table = document.getElementById(tableId);
        if (!this.table) {
            console.error(`Table with id '${tableId}' not found`);
            return;
        }

        this.options = {
            rowsPerPage: options.rowsPerPage || 10,
            pageSizes: options.pageSizes || [5, 10, 50, 100, 'All'],
            searchable: options.searchable !== false,
            sortable: options.sortable !== false,
            ...options
        };

        this.tbody = this.table.querySelector('tbody');
        this.allRows = Array.from(this.tbody.querySelectorAll('tr'));
        this.filteredRows = [...this.allRows];
        this.currentPage = 1;
        this.rowsPerPage = this.options.rowsPerPage;
        this.sortColumn = -1;
        this.sortAscending = true;

        this.init();
    }

    init() {
        this.createControls();
        if (this.options.sortable) {
            this.makeSortable();
        }
        this.render();
    }

    createControls() {
        const tableWrapper = this.table.parentElement;

        // Top controls container
        const topControls = document.createElement('div');
        topControls.className = 'table-controls-top d-flex justify-content-between align-items-center mb-3';
        topControls.innerHTML = `
            <div class="d-flex align-items-center">
                <label class="me-2 mb-0">Show</label>
                <select id="${this.table.id}-pageSize" class="form-select form-select-sm" style="width: auto;">
                    ${this.options.pageSizes.map(size =>
                        `<option value="${size}" ${size === this.rowsPerPage ? 'selected' : ''}>${size}</option>`
                    ).join('')}
                </select>
                <label class="ms-2 mb-0">entries</label>
            </div>
            ${this.options.searchable ? `
            <div>
                <input type="text" id="${this.table.id}-search" class="form-control form-control-sm"
                       placeholder="Search in table..." style="width: 250px;">
            </div>
            ` : ''}
        `;

        // Bottom controls container
        const bottomControls = document.createElement('div');
        bottomControls.className = 'table-controls-bottom d-flex justify-content-between align-items-center mt-3';
        bottomControls.innerHTML = `
            <div id="${this.table.id}-info" class="text-muted"></div>
            <nav>
                <ul id="${this.table.id}-pagination" class="pagination pagination-sm mb-0"></ul>
            </nav>
        `;

        // Insert controls
        tableWrapper.insertBefore(topControls, this.table);
        tableWrapper.appendChild(bottomControls);

        // Attach event listeners
        this.pageSizeSelect = document.getElementById(`${this.table.id}-pageSize`);
        this.pageSizeSelect.addEventListener('change', (e) => {
            this.rowsPerPage = e.target.value === 'All' ? this.filteredRows.length : parseInt(e.target.value);
            this.currentPage = 1;
            this.render();
        });

        if (this.options.searchable) {
            this.searchInput = document.getElementById(`${this.table.id}-search`);
            this.searchInput.addEventListener('input', (e) => {
                this.filter(e.target.value);
            });
        }

        this.infoDiv = document.getElementById(`${this.table.id}-info`);
        this.paginationUl = document.getElementById(`${this.table.id}-pagination`);
    }

    makeSortable() {
        const headers = this.table.querySelectorAll('thead th');
        headers.forEach((th, index) => {
            if (th.classList.contains('no-sort')) return;

            th.style.cursor = 'pointer';
            th.style.userSelect = 'none';
            th.innerHTML += ' <span class="sort-icon">⇅</span>';

            th.addEventListener('click', () => {
                this.sort(index);
            });
        });
    }

    sort(columnIndex) {
        if (this.sortColumn === columnIndex) {
            this.sortAscending = !this.sortAscending;
        } else {
            this.sortColumn = columnIndex;
            this.sortAscending = true;
        }

        // Update sort icons
        const headers = this.table.querySelectorAll('thead th');
        headers.forEach((th, index) => {
            const icon = th.querySelector('.sort-icon');
            if (icon) {
                if (index === columnIndex) {
                    icon.textContent = this.sortAscending ? '▲' : '▼';
                } else {
                    icon.textContent = '⇅';
                }
            }
        });

        // Sort rows
        this.filteredRows.sort((a, b) => {
            const aValue = a.cells[columnIndex]?.textContent.trim() || '';
            const bValue = b.cells[columnIndex]?.textContent.trim() || '';

            // Try numeric comparison first
            const aNum = parseFloat(aValue);
            const bNum = parseFloat(bValue);

            if (!isNaN(aNum) && !isNaN(bNum)) {
                return this.sortAscending ? aNum - bNum : bNum - aNum;
            }

            // String comparison
            return this.sortAscending
                ? aValue.localeCompare(bValue)
                : bValue.localeCompare(aValue);
        });

        this.currentPage = 1;
        this.render();
    }

    filter(searchTerm) {
        const term = searchTerm.toLowerCase();

        if (!term) {
            this.filteredRows = [...this.allRows];
        } else {
            this.filteredRows = this.allRows.filter(row => {
                const text = row.textContent.toLowerCase();
                return text.includes(term);
            });
        }

        this.currentPage = 1;
        this.render();
    }

    render() {
        // Clear tbody
        this.tbody.innerHTML = '';

        // Calculate pagination
        const totalRows = this.filteredRows.length;
        const totalPages = this.rowsPerPage >= totalRows ? 1 : Math.ceil(totalRows / this.rowsPerPage);

        // Ensure current page is valid
        if (this.currentPage > totalPages) {
            this.currentPage = totalPages || 1;
        }

        // Show rows for current page
        const start = (this.currentPage - 1) * this.rowsPerPage;
        const end = this.rowsPerPage >= totalRows ? totalRows : Math.min(start + this.rowsPerPage, totalRows);

        const rowsToShow = this.filteredRows.slice(start, end);
        rowsToShow.forEach(row => this.tbody.appendChild(row));

        // Show "no results" if empty
        if (this.filteredRows.length === 0) {
            const noResultsRow = document.createElement('tr');
            noResultsRow.innerHTML = `<td colspan="100" class="text-center text-muted py-4">No matching records found</td>`;
            this.tbody.appendChild(noResultsRow);
        }

        // Update info
        if (totalRows > 0) {
            this.infoDiv.textContent = `Showing ${start + 1} to ${end} of ${totalRows} entries`;
        } else {
            this.infoDiv.textContent = 'Showing 0 entries';
        }

        // Update pagination
        this.renderPagination(totalPages);
    }

    renderPagination(totalPages) {
        this.paginationUl.innerHTML = '';

        if (totalPages <= 1) return;

        // Previous button
        const prevLi = document.createElement('li');
        prevLi.className = `page-item ${this.currentPage === 1 ? 'disabled' : ''}`;
        prevLi.innerHTML = `<a class="page-link" href="#">Previous</a>`;
        prevLi.addEventListener('click', (e) => {
            e.preventDefault();
            if (this.currentPage > 1) {
                this.currentPage--;
                this.render();
            }
        });
        this.paginationUl.appendChild(prevLi);

        // Page numbers
        const maxButtons = 5;
        let startPage = Math.max(1, this.currentPage - Math.floor(maxButtons / 2));
        let endPage = Math.min(totalPages, startPage + maxButtons - 1);

        if (endPage - startPage < maxButtons - 1) {
            startPage = Math.max(1, endPage - maxButtons + 1);
        }

        // First page
        if (startPage > 1) {
            this.addPageButton(1);
            if (startPage > 2) {
                const ellipsisLi = document.createElement('li');
                ellipsisLi.className = 'page-item disabled';
                ellipsisLi.innerHTML = '<a class="page-link">...</a>';
                this.paginationUl.appendChild(ellipsisLi);
            }
        }

        // Page buttons
        for (let i = startPage; i <= endPage; i++) {
            this.addPageButton(i);
        }

        // Last page
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                const ellipsisLi = document.createElement('li');
                ellipsisLi.className = 'page-item disabled';
                ellipsisLi.innerHTML = '<a class="page-link">...</a>';
                this.paginationUl.appendChild(ellipsisLi);
            }
            this.addPageButton(totalPages);
        }

        // Next button
        const nextLi = document.createElement('li');
        nextLi.className = `page-item ${this.currentPage === totalPages ? 'disabled' : ''}`;
        nextLi.innerHTML = `<a class="page-link" href="#">Next</a>`;
        nextLi.addEventListener('click', (e) => {
            e.preventDefault();
            if (this.currentPage < totalPages) {
                this.currentPage++;
                this.render();
            }
        });
        this.paginationUl.appendChild(nextLi);
    }

    addPageButton(pageNum) {
        const li = document.createElement('li');
        li.className = `page-item ${pageNum === this.currentPage ? 'active' : ''}`;
        li.innerHTML = `<a class="page-link" href="#">${pageNum}</a>`;
        li.addEventListener('click', (e) => {
            e.preventDefault();
            this.currentPage = pageNum;
            this.render();
        });
        this.paginationUl.appendChild(li);
    }
}

// Make it globally available
window.EnhancedTable = EnhancedTable;