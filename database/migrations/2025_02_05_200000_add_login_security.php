<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->integer('login_attempts')->default(0)->after('remember_token');
            $table->timestamp('locked_until')->nullable()->after('login_attempts');
            $table->timestamp('last_failed_at')->nullable()->after('locked_until');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropColumn(['login_attempts', 'locked_until', 'last_failed_at']);
        });
    }
};
